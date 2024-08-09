/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.taskbar.bubbles.stashing

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.res.Resources
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import com.android.launcher3.R
import com.android.launcher3.anim.AnimatedFloat
import com.android.launcher3.taskbar.StashedHandleViewController
import com.android.launcher3.taskbar.TaskbarInsetsController
import com.android.launcher3.taskbar.bubbles.BubbleBarViewController
import com.android.launcher3.taskbar.bubbles.BubbleStashedHandleViewController
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_STASH_DURATION
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_TRANSLATION_DURATION
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.STASHED_BAR_SCALE
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.ControllersAfterInitAction
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.TaskbarHotseatDimensionsProvider
import com.android.launcher3.util.MultiPropertyFactory
import com.android.wm.shell.common.bubbles.BubbleBarLocation
import com.android.wm.shell.shared.animation.PhysicsAnimator

class TransientBubbleStashController(
    private val taskbarHotseatDimensionsProvider: TaskbarHotseatDimensionsProvider,
    resources: Resources
) : BubbleStashController {

    private lateinit var bubbleBarViewController: BubbleBarViewController
    private lateinit var taskbarInsetsController: TaskbarInsetsController
    private lateinit var controllersAfterInitAction: ControllersAfterInitAction

    // stash view properties
    private var bubbleStashedHandleViewController: BubbleStashedHandleViewController? = null
    private var stashHandleViewAlpha: MultiPropertyFactory<View>.MultiProperty? = null
    private var stashedHeight: Int = 0

    // bubble bar properties
    private lateinit var bubbleBarAlpha: MultiPropertyFactory<View>.MultiProperty
    private lateinit var bubbleBarTranslationYAnimator: AnimatedFloat
    private lateinit var bubbleBarScale: AnimatedFloat
    private val mHandleCenterFromScreenBottom =
        resources.getDimensionPixelSize(R.dimen.bubblebar_stashed_size) / 2f

    private var animator: AnimatorSet? = null

    override var isStashed: Boolean = false
        @VisibleForTesting set

    override var isBubblesShowingOnHome: Boolean = false
        set(onHome) {
            if (field == onHome) return
            field = onHome
            if (!bubbleBarViewController.hasBubbles()) {
                // if there are no bubbles, there's nothing to show, so just return.
                return
            }
            if (onHome) {
                updateStashedAndExpandedState(stash = false, expand = false)
                // When transitioning from app to home we need to animate the bubble bar
                // here to align with hotseat center.
                animateBubbleBarYToHotseat()
            } else if (!bubbleBarViewController.isExpanded) {
                updateStashedAndExpandedState(stash = true, expand = false)
            }
            bubbleBarViewController.onBubbleBarConfigurationChanged(/* animate= */ true)
        }

    override var isBubblesShowingOnOverview: Boolean = false
        set(onOverview) {
            if (field == onOverview) return
            field = onOverview
            if (onOverview) {
                // When transitioning to overview we need to animate the bubble bar to align with
                // the taskbar bottom.
                animateBubbleBarYToTaskbar()
            } else {
                updateStashedAndExpandedState(stash = true, expand = false)
            }
            bubbleBarViewController.onBubbleBarConfigurationChanged(/* animate= */ true)
        }

    override var isSysuiLocked: Boolean = false
        set(isLocked) {
            if (field == isLocked) return
            field = isLocked
            if (!isLocked && bubbleBarViewController.hasBubbles()) {
                animateAfterUnlock()
            }
        }

    override val isTransientTaskBar: Boolean = true

    override val bubbleBarTranslationYForHotseat: Float
        get() {
            val hotseatBottomSpace = taskbarHotseatDimensionsProvider.getHotseatBottomSpace()
            val hotseatCellHeight = taskbarHotseatDimensionsProvider.getHotseatHeight()
            val bubbleBarHeight: Float = bubbleBarViewController.bubbleBarCollapsedHeight
            return -hotseatBottomSpace - (hotseatCellHeight - bubbleBarHeight) / 2
        }

    override val bubbleBarTranslationYForTaskbar: Float =
        -taskbarHotseatDimensionsProvider.getTaskbarBottomSpace().toFloat()

    /** Check if we have handle view controller */
    override val hasHandleView: Boolean
        get() = bubbleStashedHandleViewController != null

    override fun init(
        taskbarInsetsController: TaskbarInsetsController,
        bubbleBarViewController: BubbleBarViewController,
        bubbleStashedHandleViewController: BubbleStashedHandleViewController?,
        controllersAfterInitAction: ControllersAfterInitAction
    ) {
        this.taskbarInsetsController = taskbarInsetsController
        this.bubbleBarViewController = bubbleBarViewController
        this.bubbleStashedHandleViewController = bubbleStashedHandleViewController
        this.controllersAfterInitAction = controllersAfterInitAction
        bubbleBarTranslationYAnimator = bubbleBarViewController.bubbleBarTranslationY
        // bubble bar has only alpha property, getting it at index 0
        bubbleBarAlpha = bubbleBarViewController.bubbleBarAlpha.get(/* index= */ 0)
        bubbleBarScale = bubbleBarViewController.bubbleBarScale
        stashedHeight = bubbleStashedHandleViewController?.stashedHeight ?: 0
        stashHandleViewAlpha =
            bubbleStashedHandleViewController
                ?.stashedHandleAlpha
                ?.get(StashedHandleViewController.ALPHA_INDEX_STASHED)
    }

    private fun animateAfterUnlock() {
        val animatorSet = AnimatorSet()
        if (isBubblesShowingOnHome || isBubblesShowingOnOverview) {
            isStashed = false
            animatorSet.playTogether(
                bubbleBarScale.animateToValue(1f),
                bubbleBarTranslationYAnimator.animateToValue(bubbleBarTranslationY),
                bubbleBarAlpha.animateToValue(1f)
            )
        } else {
            isStashed = true
            stashHandleViewAlpha?.let { animatorSet.playTogether(it.animateToValue(1f)) }
        }
        animatorSet.updateTouchRegionOnAnimationEnd().setDuration(BAR_STASH_DURATION).start()
    }

    override fun showBubbleBarImmediate() {
        showBubbleBarImmediate(bubbleBarTranslationY)
    }

    override fun showBubbleBarImmediate(bubbleBarTranslationY: Float) {
        bubbleStashedHandleViewController?.setTranslationYForSwipe(0f)
        stashHandleViewAlpha?.value = 0f
        this.bubbleBarTranslationYAnimator.updateValue(bubbleBarTranslationY)
        bubbleBarAlpha.setValue(1f)
        bubbleBarScale.updateValue(1f)
        isStashed = false
        onIsStashedChanged()
    }

    override fun stashBubbleBarImmediate() {
        bubbleStashedHandleViewController?.setTranslationYForSwipe(0f)
        stashHandleViewAlpha?.value = 1f
        this.bubbleBarTranslationYAnimator.updateValue(getStashTranslation())
        bubbleBarAlpha.setValue(0f)
        bubbleBarScale.updateValue(STASHED_BAR_SCALE)
        isStashed = true
        onIsStashedChanged()
    }

    override fun getTouchableHeight(): Int =
        when {
            isStashed -> stashedHeight
            isBubbleBarVisible() -> bubbleBarViewController.bubbleBarCollapsedHeight.toInt()
            else -> 0
        }

    override fun isBubbleBarVisible(): Boolean = bubbleBarViewController.hasBubbles() && !isStashed

    override fun onNewBubbleAnimationInterrupted(isStashed: Boolean, bubbleBarTranslationY: Float) =
        if (isStashed) {
            stashBubbleBarImmediate()
        } else {
            showBubbleBarImmediate(bubbleBarTranslationY)
        }

    /** Check if [ev] belongs to the stash handle or the bubble bar views. */
    override fun isEventOverBubbleBarViews(ev: MotionEvent): Boolean {
        val isOverHandle = bubbleStashedHandleViewController?.isEventOverHandle(ev) ?: false
        return isOverHandle || bubbleBarViewController.isEventOverAnyItem(ev)
    }

    /** Set the bubble bar stash handle location . */
    override fun setBubbleBarLocation(bubbleBarLocation: BubbleBarLocation) {
        bubbleStashedHandleViewController?.setBubbleBarLocation(bubbleBarLocation)
    }

    override fun stashBubbleBar() {
        updateStashedAndExpandedState(stash = true, expand = false)
    }

    override fun showBubbleBar(expandBubbles: Boolean) {
        updateStashedAndExpandedState(stash = false, expandBubbles)
    }

    override fun getDiffBetweenHandleAndBarCenters(): Float {
        // the difference between the centers of the handle and the bubble bar is the difference
        // between their distance from the bottom of the screen.
        val barCenter: Float = bubbleBarViewController.bubbleBarCollapsedHeight / 2f
        return mHandleCenterFromScreenBottom - barCenter
    }

    override fun getStashedHandleTranslationForNewBubbleAnimation(): Float {
        return -mHandleCenterFromScreenBottom
    }

    override fun getStashedHandlePhysicsAnimator(): PhysicsAnimator<View>? {
        return bubbleStashedHandleViewController?.physicsAnimator
    }

    override fun updateTaskbarTouchRegion() {
        taskbarInsetsController.onTaskbarOrBubblebarWindowHeightOrInsetsChanged()
    }

    override fun setHandleTranslationY(translationY: Float) {
        bubbleStashedHandleViewController?.setTranslationYForSwipe(translationY)
    }

    override fun getHandleTranslationY(): Float? = bubbleStashedHandleViewController?.translationY

    private fun getStashTranslation(): Float {
        return (bubbleBarViewController.bubbleBarCollapsedHeight - stashedHeight) / 2f
    }

    /**
     * Create a stash animation.
     *
     * @param isStashed whether it's a stash animation or an unstash animation
     * @param duration duration of the animation
     * @return the animation
     */
    @Suppress("SameParameterValue")
    private fun createStashAnimator(isStashed: Boolean, duration: Long): AnimatorSet {
        val animatorSet = AnimatorSet()
        val fullLengthAnimatorSet = AnimatorSet()
        // Not exactly half and may overlap. See [first|second]HalfDurationScale below.
        val firstHalfAnimatorSet = AnimatorSet()
        val secondHalfAnimatorSet = AnimatorSet()
        val firstHalfDurationScale: Float
        val secondHalfDurationScale: Float
        val stashHandleAlphaValue: Float
        if (isStashed) {
            firstHalfDurationScale = 0.75f
            secondHalfDurationScale = 0.5f
            stashHandleAlphaValue = 1f
            fullLengthAnimatorSet.play(
                bubbleBarTranslationYAnimator.animateToValue(getStashTranslation())
            )
            firstHalfAnimatorSet.playTogether(
                bubbleBarAlpha.animateToValue(0f),
                bubbleBarScale.animateToValue(STASHED_BAR_SCALE)
            )
        } else {
            firstHalfDurationScale = 0.5f
            secondHalfDurationScale = 0.75f
            stashHandleAlphaValue = 0f
            fullLengthAnimatorSet.playTogether(
                bubbleBarScale.animateToValue(1f),
                bubbleBarTranslationYAnimator.animateToValue(bubbleBarTranslationY)
            )
            secondHalfAnimatorSet.playTogether(bubbleBarAlpha.animateToValue(1f))
        }
        stashHandleViewAlpha?.let {
            secondHalfAnimatorSet.playTogether(it.animateToValue(stashHandleAlphaValue))
        }
        bubbleStashedHandleViewController?.createRevealAnimToIsStashed(isStashed)?.let {
            fullLengthAnimatorSet.play(it)
        }
        fullLengthAnimatorSet.setDuration(duration)
        firstHalfAnimatorSet.setDuration((duration * firstHalfDurationScale).toLong())
        secondHalfAnimatorSet.setDuration((duration * secondHalfDurationScale).toLong())
        secondHalfAnimatorSet.startDelay = (duration * (1 - secondHalfDurationScale)).toLong()
        animatorSet.playTogether(fullLengthAnimatorSet, firstHalfAnimatorSet, secondHalfAnimatorSet)
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    controllersAfterInitAction.runAfterInit {
                        if (isStashed) {
                            bubbleBarViewController.isExpanded = false
                        }
                        taskbarInsetsController.onTaskbarOrBubblebarWindowHeightOrInsetsChanged()
                    }
                }
            }
        )
        return animatorSet
    }

    private fun onIsStashedChanged() {
        controllersAfterInitAction.runAfterInit {
            taskbarInsetsController.onTaskbarOrBubblebarWindowHeightOrInsetsChanged()
            bubbleStashedHandleViewController?.onIsStashedChanged()
        }
    }

    private fun animateBubbleBarYToHotseat() {
        translateBubbleBarYUpdateTouchRegionOnCompletion(bubbleBarTranslationYForHotseat)
    }

    private fun animateBubbleBarYToTaskbar() {
        translateBubbleBarYUpdateTouchRegionOnCompletion(bubbleBarTranslationYForTaskbar)
    }

    private fun translateBubbleBarYUpdateTouchRegionOnCompletion(toY: Float) {
        bubbleBarViewController.bubbleBarTranslationY
            .animateToValue(toY)
            .updateTouchRegionOnAnimationEnd()
            .setDuration(BAR_TRANSLATION_DURATION)
            .start()
    }

    @VisibleForTesting
    fun updateStashedAndExpandedState(stash: Boolean, expand: Boolean) {
        if (bubbleBarViewController.isHiddenForNoBubbles) {
            // If there are no bubbles the bar and handle are invisible, nothing to do here.
            return
        }
        val isStashed = stash && !isBubblesShowingOnHome && !isBubblesShowingOnOverview
        if (this.isStashed != isStashed) {
            this.isStashed = isStashed
            // notify the view controller that the stash state is about to change so that it can
            // cancel an ongoing animation if there is one.
            // note that this has to be called before updating mIsStashed with the new value,
            // otherwise interrupting an ongoing animation may update it again with the wrong state
            bubbleBarViewController.onStashStateChanging()
            animator?.cancel()
            animator =
                createStashAnimator(isStashed, BAR_STASH_DURATION).apply {
                    updateTouchRegionOnAnimationEnd()
                    start()
                }
        }
        if (bubbleBarViewController.isExpanded != expand) {
            bubbleBarViewController.isExpanded = expand
        }
    }

    private fun Animator.updateTouchRegionOnAnimationEnd(): Animator {
        this.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onIsStashedChanged()
                }
            }
        )
        return this
    }
}
