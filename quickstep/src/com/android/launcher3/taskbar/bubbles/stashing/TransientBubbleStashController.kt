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
import android.animation.AnimatorSet
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.dynamicanimation.animation.SpringForce
import com.android.app.animation.Interpolators.EMPHASIZED
import com.android.app.animation.Interpolators.LINEAR
import com.android.launcher3.R
import com.android.launcher3.anim.AnimatedFloat
import com.android.launcher3.anim.SpringAnimationBuilder
import com.android.launcher3.taskbar.TaskbarInsetsController
import com.android.launcher3.taskbar.bubbles.BubbleBarViewController
import com.android.launcher3.taskbar.bubbles.BubbleStashedHandleViewController
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_STASH_ALPHA_DELAY
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_STASH_ALPHA_DURATION
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_STASH_DURATION
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.Companion.BAR_TRANSLATION_DURATION
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.ControllersAfterInitAction
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController.TaskbarHotseatDimensionsProvider
import com.android.launcher3.util.MultiPropertyFactory
import com.android.wm.shell.shared.animation.PhysicsAnimator
import com.android.wm.shell.shared.bubbles.BubbleBarLocation
import kotlin.math.max

class TransientBubbleStashController(
    private val taskbarHotseatDimensionsProvider: TaskbarHotseatDimensionsProvider,
    private val context: Context
) : BubbleStashController {

    private lateinit var bubbleBarViewController: BubbleBarViewController
    private lateinit var taskbarInsetsController: TaskbarInsetsController
    private lateinit var controllersAfterInitAction: ControllersAfterInitAction

    // stash view properties
    private var bubbleStashedHandleViewController: BubbleStashedHandleViewController? = null
    private var stashHandleViewAlpha: MultiPropertyFactory<View>.MultiProperty? = null
    private var translationYDuringStash = AnimatedFloat { transY ->
        bubbleStashedHandleViewController?.setTranslationYForStash(transY)
        bubbleBarViewController.setTranslationYForStash(transY)
    }
    private val stashHandleStashVelocity =
        context.resources.getDimension(R.dimen.bubblebar_stashed_handle_spring_velocity_dp_per_s)
    private var stashedHeight: Int = 0

    // bubble bar properties
    private lateinit var bubbleBarAlpha: MultiPropertyFactory<View>.MultiProperty
    private lateinit var bubbleBarTranslationYAnimator: AnimatedFloat
    private lateinit var bubbleBarScaleX: AnimatedFloat
    private lateinit var bubbleBarScaleY: AnimatedFloat
    private val handleCenterFromScreenBottom =
        context.resources.getDimensionPixelSize(R.dimen.bubblebar_stashed_size) / 2f

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
        bubbleBarScaleX = bubbleBarViewController.bubbleBarScaleX
        bubbleBarScaleY = bubbleBarViewController.bubbleBarScaleY
        stashedHeight = bubbleStashedHandleViewController?.stashedHeight ?: 0
        stashHandleViewAlpha = bubbleStashedHandleViewController?.stashedHandleAlpha?.get(0)
    }

    private fun animateAfterUnlock() {
        val animatorSet = AnimatorSet()
        if (isBubblesShowingOnHome || isBubblesShowingOnOverview) {
            isStashed = false
            animatorSet.playTogether(
                bubbleBarScaleX.animateToValue(1f),
                bubbleBarScaleY.animateToValue(1f),
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
        bubbleBarScaleX.updateValue(1f)
        bubbleBarScaleY.updateValue(1f)
        isStashed = false
        onIsStashedChanged()
    }

    override fun stashBubbleBarImmediate() {
        bubbleStashedHandleViewController?.setTranslationYForSwipe(0f)
        stashHandleViewAlpha?.value = 1f
        this.bubbleBarTranslationYAnimator.updateValue(getStashTranslation())
        bubbleBarAlpha.setValue(0f)
        bubbleBarScaleX.updateValue(getStashScaleX())
        bubbleBarScaleY.updateValue(getStashScaleY())
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
        return handleCenterFromScreenBottom - barCenter
    }

    override fun getStashedHandleTranslationForNewBubbleAnimation(): Float {
        return -handleCenterFromScreenBottom
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
        return bubbleBarTranslationY / 2f
    }

    @VisibleForTesting
    fun getStashScaleX(): Float {
        val handleWidth = bubbleStashedHandleViewController?.stashedWidth ?: 0
        return handleWidth / bubbleBarViewController.bubbleBarCollapsedWidth
    }

    @VisibleForTesting
    fun getStashScaleY(): Float {
        val handleHeight = bubbleStashedHandleViewController?.stashedHeight ?: 0
        return handleHeight / bubbleBarViewController.bubbleBarCollapsedHeight
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

        val alphaDuration = if (isStashed) duration else BAR_STASH_ALPHA_DURATION
        val alphaDelay = if (isStashed) BAR_STASH_ALPHA_DELAY else 0L
        animatorSet.play(
            createStashAlphaAnimator(isStashed).apply {
                this.duration = max(0L, alphaDuration - alphaDelay)
                this.startDelay = alphaDelay
                this.interpolator = LINEAR
            }
        )

        animatorSet.play(
            createSpringOnStashAnimator(isStashed).apply {
                this.duration = duration
                this.interpolator = LINEAR
            }
        )

        animatorSet.play(
            bubbleStashedHandleViewController?.createRevealAnimToIsStashed(isStashed)?.apply {
                this.duration = duration
                this.interpolator = EMPHASIZED
            }
        )

        val pivotX = if (bubbleBarViewController.isBubbleBarOnLeft) 0f else 1f
        animatorSet.play(
            createScaleAnimator(isStashed).apply {
                this.duration = duration
                this.interpolator = EMPHASIZED
                this.setBubbleBarPivotDuringAnim(pivotX, 1f)
            }
        )

        val translationYTarget = if (isStashed) getStashTranslation() else bubbleBarTranslationY
        animatorSet.play(
            bubbleBarTranslationYAnimator.animateToValue(translationYTarget).apply {
                this.duration = duration
                this.interpolator = EMPHASIZED
            }
        )

        animatorSet.doOnEnd {
            animator = null
            controllersAfterInitAction.runAfterInit {
                if (isStashed) {
                    bubbleBarViewController.isExpanded = false
                }
                taskbarInsetsController.onTaskbarOrBubblebarWindowHeightOrInsetsChanged()
            }
        }
        return animatorSet
    }

    private fun createStashAlphaAnimator(isStashed: Boolean): AnimatorSet {
        val stashHandleAlphaTarget = if (isStashed) 1f else 0f
        val barAlphaTarget = if (isStashed) 0f else 1f
        return AnimatorSet().apply {
            play(bubbleBarAlpha.animateToValue(barAlphaTarget))
            play(stashHandleViewAlpha?.animateToValue(stashHandleAlphaTarget))
        }
    }

    private fun createSpringOnStashAnimator(isStashed: Boolean): Animator {
        if (!isStashed) {
            // Animate the stash translation back to 0
            return translationYDuringStash.animateToValue(0f)
        }
        // Apply a spring to the handle
        return SpringAnimationBuilder(context)
            .setStartValue(translationYDuringStash.value)
            .setEndValue(0f)
            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
            .setStiffness(SpringForce.STIFFNESS_LOW)
            .setStartVelocity(stashHandleStashVelocity)
            .build(translationYDuringStash, AnimatedFloat.VALUE)
    }

    private fun createScaleAnimator(isStashed: Boolean): AnimatorSet {
        val scaleXTarget = if (isStashed) getStashScaleX() else 1f
        val scaleYTarget = if (isStashed) getStashScaleY() else 1f
        return AnimatorSet().apply {
            play(bubbleBarScaleX.animateToValue(scaleXTarget))
            play(bubbleBarScaleY.animateToValue(scaleYTarget))
        }
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
        doOnEnd { onIsStashedChanged() }
        return this
    }

    private fun Animator.setBubbleBarPivotDuringAnim(pivotX: Float, pivotY: Float): Animator {
        var initialPivotX = Float.NaN
        var initialPivotY = Float.NaN
        doOnStart {
            initialPivotX = bubbleBarViewController.bubbleBarRelativePivotX
            initialPivotY = bubbleBarViewController.bubbleBarRelativePivotY
            bubbleBarViewController.setBubbleBarRelativePivot(pivotX, pivotY)
        }
        doOnEnd {
            if (!initialPivotX.isNaN() && !initialPivotY.isNaN()) {
                bubbleBarViewController.setBubbleBarRelativePivot(initialPivotX, initialPivotY)
            }
        }
        return this
    }
}
