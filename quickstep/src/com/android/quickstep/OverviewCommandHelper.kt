/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.quickstep

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.PointF
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import android.view.View
import androidx.annotation.BinderThread
import androidx.annotation.UiThread
import com.android.internal.jank.Cuj
import com.android.launcher3.PagedView
import com.android.launcher3.config.FeatureFlags
import com.android.launcher3.logger.LauncherAtom
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.*
import com.android.launcher3.util.Executors
import com.android.launcher3.util.RunnableList
import com.android.quickstep.util.ActiveGestureLog
import com.android.quickstep.views.RecentsView
import com.android.quickstep.views.RecentsViewContainer
import com.android.quickstep.views.TaskView
import com.android.systemui.shared.recents.model.ThumbnailData
import com.android.systemui.shared.system.InteractionJankMonitorWrapper
import java.io.PrintWriter

/** Helper class to handle various atomic commands for switching between Overview. */
class OverviewCommandHelper(
    private val touchInteractionService: TouchInteractionService,
    private val overviewComponentObserver: OverviewComponentObserver,
    private val taskAnimationManager: TaskAnimationManager
) {
    private val pendingCommands = mutableListOf<CommandInfo>()

    /**
     * Index of the TaskView that should be focused when launching Overview. Persisted so that we do
     * not lose the focus across multiple calls of [OverviewCommandHelper.executeCommand] for the
     * same command
     */
    private var keyboardTaskFocusIndex = -1

    /**
     * Whether we should incoming toggle commands while a previous toggle command is still ongoing.
     * This serves as a rate-limiter to prevent overlapping animations that can clobber each other
     * and prevent clean-up callbacks from running. This thus prevents a recurring set of bugs with
     * janky recents animations and unresponsive home and overview buttons.
     */
    private var waitForToggleCommandComplete = false

    /** Called when the command finishes execution. */
    private fun scheduleNextTask(command: CommandInfo) {
        if (pendingCommands.isEmpty()) {
            Log.d(TAG, "no pending commands to schedule")
            return
        }
        if (pendingCommands.first() !== command) {
            Log.d(
                TAG,
                "next task not scheduled. First pending command type " +
                    "is ${pendingCommands.first()} - command type is: $command"
            )
            return
        }
        Log.d(TAG, "scheduleNextTask called: $command")
        pendingCommands.removeFirst()
        executeNext()
    }

    /**
     * Executes the next command from the queue. If the command finishes immediately (returns true),
     * it continues to execute the next command, until the queue is empty of a command defer's its
     * completion (returns false).
     */
    @UiThread
    private fun executeNext() {
        if (pendingCommands.isEmpty()) {
            Log.d(TAG, "executeNext - pendingCommands is empty")
            return
        }
        val command = pendingCommands.first()
        val result = executeCommand(command)
        Log.d(TAG, "executeNext command type: $command, result: $result")
        if (result) {
            scheduleNextTask(command)
        }
    }

    @UiThread
    private fun addCommand(command: CommandInfo) {
        val wasEmpty = pendingCommands.isEmpty()
        pendingCommands.add(command)
        if (wasEmpty) {
            executeNext()
        }
    }

    /**
     * Adds a command to be executed next, after all pending tasks are completed. Max commands that
     * can be queued is [.MAX_QUEUE_SIZE]. Requests after reaching that limit will be silently
     * dropped.
     */
    @BinderThread
    fun addCommand(type: Int) {
        if (pendingCommands.size >= MAX_QUEUE_SIZE) {
            Log.d(
                TAG,
                "the pending command queue is full (${pendingCommands.size}). command not added: $type"
            )
            return
        }
        Log.d(TAG, "adding command type: $type")
        val command = CommandInfo(type)
        Executors.MAIN_EXECUTOR.execute { addCommand(command) }
    }

    @UiThread
    fun clearPendingCommands() {
        Log.d(TAG, "clearing pending commands - size: ${pendingCommands.size}")
        pendingCommands.clear()
    }

    @UiThread
    fun canStartHomeSafely(): Boolean =
        pendingCommands.isEmpty() || pendingCommands.first().type == TYPE_HOME

    private fun getNextTask(view: RecentsView<*, *>): TaskView? {
        val runningTaskView = view.runningTaskView

        return if (runningTaskView == null) {
            view.getTaskViewAt(0)
        } else {
            val nextTask = view.nextTaskView
            nextTask ?: runningTaskView
        }
    }

    private fun launchTask(
        recents: RecentsView<*, *>,
        taskView: TaskView?,
        command: CommandInfo
    ): Boolean {
        var callbackList: RunnableList? = null
        if (taskView != null) {
            waitForToggleCommandComplete = true
            taskView.isEndQuickSwitchCuj = true
            callbackList = taskView.launchTasks()
        }

        if (callbackList != null) {
            callbackList.add {
                Log.d(TAG, "launching task callback: $command")
                scheduleNextTask(command)
                waitForToggleCommandComplete = false
            }
            Log.d(TAG, "launching task - waiting for callback: $command")
            return false
        } else {
            recents.startHome()
            waitForToggleCommandComplete = false
            return true
        }
    }

    /**
     * Executes the task and returns true if next task can be executed. If false, then the next task
     * is deferred until [.scheduleNextTask] is called
     */
    private fun executeCommand(command: CommandInfo): Boolean {
        if (waitForToggleCommandComplete && command.type == TYPE_TOGGLE) {
            Log.d(TAG, "executeCommand: $command - waiting for toggle command complete")
            return true
        }
        val activityInterface: BaseActivityInterface<*, *> =
            overviewComponentObserver.activityInterface

        val visibleRecentsView: RecentsView<*, *>? =
            activityInterface.getVisibleRecentsView<RecentsView<*, *>>()
        val createdRecentsView: RecentsView<*, *>?

        Log.d(TAG, "executeCommand: $command - visibleRecentsView: $visibleRecentsView")
        if (visibleRecentsView == null) {
            val activity = activityInterface.getCreatedContainer() as? RecentsViewContainer
            createdRecentsView = activity?.getOverviewPanel()
            val deviceProfile = activity?.getDeviceProfile()
            val uiController = activityInterface.getTaskbarController()
            val allowQuickSwitch =
                FeatureFlags.ENABLE_KEYBOARD_QUICK_SWITCH.get() &&
                    uiController != null &&
                    deviceProfile != null &&
                    (deviceProfile.isTablet || deviceProfile.isTwoPanels)

            when (command.type) {
                TYPE_HIDE -> {
                    if (!allowQuickSwitch) return true
                    keyboardTaskFocusIndex = uiController!!.launchFocusedTask()
                    if (keyboardTaskFocusIndex == -1) return true
                }
                TYPE_KEYBOARD_INPUT ->
                    if (allowQuickSwitch) {
                        uiController!!.openQuickSwitchView()
                        return true
                    } else {
                        keyboardTaskFocusIndex = 0
                    }
                TYPE_HOME -> {
                    ActiveGestureLog.INSTANCE.addLog(
                        "OverviewCommandHelper.executeCommand(TYPE_HOME)"
                    )
                    // Although IActivityTaskManager$Stub$Proxy.startActivity is a slow binder call,
                    // we should still call it on main thread because launcher is waiting for
                    // ActivityTaskManager to resume it. Also calling startActivity() on bg thread
                    // could potentially delay resuming launcher. See b/348668521 for more details.
                    touchInteractionService.startActivity(overviewComponentObserver.homeIntent)
                    return true
                }
                TYPE_SHOW ->
                    // When Recents is not currently visible, the command's type is
                    // TYPE_SHOW
                    // when overview is triggered via the keyboard overview button or Action+Tab
                    // keys (Not Alt+Tab which is KQS). The overview button on-screen in 3-button
                    // nav is TYPE_TOGGLE.
                    keyboardTaskFocusIndex = 0
                else -> {}
            }
        } else {
            createdRecentsView = visibleRecentsView
            when (command.type) {
                TYPE_SHOW -> return true // already visible
                TYPE_KEYBOARD_INPUT,
                TYPE_HIDE -> {
                    if (visibleRecentsView.isHandlingTouch) return true

                    keyboardTaskFocusIndex = PagedView.INVALID_PAGE
                    val currentPage = visibleRecentsView.nextPage
                    val taskView = visibleRecentsView.getTaskViewAt(currentPage)
                    return launchTask(visibleRecentsView, taskView, command)
                }
                TYPE_TOGGLE ->
                    return launchTask(visibleRecentsView, getNextTask(visibleRecentsView), command)
                TYPE_HOME -> {
                    visibleRecentsView.startHome()
                    return true
                }
            }
        }

        createdRecentsView?.setKeyboardTaskFocusIndex(keyboardTaskFocusIndex)
        // Handle recents view focus when launching from home
        val animatorListener: Animator.AnimatorListener =
            object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    updateRecentsViewFocus(command)
                    logShowOverviewFrom(command.type)
                }

                override fun onAnimationEnd(animation: Animator) {
                    Log.d(TAG, "switching to Overview state - onAnimationEnd: $command")
                    super.onAnimationEnd(animation)
                    onRecentsViewFocusUpdated(command)
                    scheduleNextTask(command)
                }
            }
        if (activityInterface.switchToRecentsIfVisible(animatorListener)) {
            Log.d(TAG, "switching to Overview state - waiting: $command")
            // If successfully switched, wait until animation finishes
            return false
        }

        val activity = activityInterface.getCreatedContainer()
        if (activity != null) {
            InteractionJankMonitorWrapper.begin(activity.rootView, Cuj.CUJ_LAUNCHER_QUICK_SWITCH)
        }

        val gestureState =
            touchInteractionService.createGestureState(
                GestureState.DEFAULT_STATE,
                GestureState.TrackpadGestureType.NONE
            )
        gestureState.isHandlingAtomicEvent = true
        val interactionHandler =
            touchInteractionService.swipeUpHandlerFactory.newHandler(
                gestureState,
                command.createTime
            )
        interactionHandler.setGestureEndCallback {
            onTransitionComplete(command, interactionHandler)
        }
        interactionHandler.initWhenReady("OverviewCommandHelper: command.type=${command.type}")

        val recentAnimListener: RecentsAnimationCallbacks.RecentsAnimationListener =
            object : RecentsAnimationCallbacks.RecentsAnimationListener {
                override fun onRecentsAnimationStart(
                    controller: RecentsAnimationController,
                    targets: RecentsAnimationTargets
                ) {
                    updateRecentsViewFocus(command)
                    logShowOverviewFrom(command.type)
                    activityInterface.runOnInitBackgroundStateUI {
                        interactionHandler.onGestureEnded(0f, PointF())
                    }
                    command.removeListener(this)
                }

                override fun onRecentsAnimationCanceled(
                    thumbnailDatas: HashMap<Int, ThumbnailData>
                ) {
                    interactionHandler.onGestureCancelled()
                    command.removeListener(this)

                    activityInterface.getCreatedContainer() ?: return
                    createdRecentsView?.onRecentsAnimationComplete()
                }
            }

        // TODO(b/361768912): Dead code. Remove or update after this bug is fixed.
        //        if (visibleRecentsView != null) {
        //            visibleRecentsView.moveRunningTaskToFront();
        //        }

        if (taskAnimationManager.isRecentsAnimationRunning) {
            command.setAnimationCallbacks(
                taskAnimationManager.continueRecentsAnimation(gestureState)
            )
            command.addListener(interactionHandler)
            taskAnimationManager.notifyRecentsAnimationState(interactionHandler)
            interactionHandler.onGestureStarted(true /*isLikelyToStartNewTask*/)

            command.addListener(recentAnimListener)
            taskAnimationManager.notifyRecentsAnimationState(recentAnimListener)
        } else {
            val intent =
                Intent(interactionHandler.launchIntent)
                    .putExtra(ActiveGestureLog.INTENT_EXTRA_LOG_TRACE_ID, gestureState.gestureId)
            command.setAnimationCallbacks(
                taskAnimationManager.startRecentsAnimation(gestureState, intent, interactionHandler)
            )
            interactionHandler.onGestureStarted(false /*isLikelyToStartNewTask*/)
            command.addListener(recentAnimListener)
        }
        Trace.beginAsyncSection(TRANSITION_NAME, 0)
        Log.d(TAG, "switching via recents animation - onGestureStarted: $command")
        return false
    }

    private fun onTransitionComplete(command: CommandInfo, handler: AbsSwipeUpHandler<*, *, *>) {
        Log.d(TAG, "switching via recents animation - onTransitionComplete: $command")
        command.removeListener(handler)
        Trace.endAsyncSection(TRANSITION_NAME, 0)
        onRecentsViewFocusUpdated(command)
        scheduleNextTask(command)
    }

    private fun updateRecentsViewFocus(command: CommandInfo) {
        val recentsView: RecentsView<*, *> =
            overviewComponentObserver.activityInterface.getVisibleRecentsView() ?: return
        if (
            command.type != TYPE_KEYBOARD_INPUT &&
                command.type != TYPE_HIDE &&
                command.type != TYPE_SHOW
        ) {
            return
        }

        // When the overview is launched via alt tab (command type is TYPE_KEYBOARD_INPUT),
        // the touch mode somehow is not change to false by the Android framework.
        // The subsequent tab to go through tasks in overview can only be dispatched to
        // focuses views, while focus can only be requested in
        // {@link View#requestFocusNoSearch(int, Rect)} when touch mode is false. To note,
        // here we launch overview with live tile.
        recentsView.viewRootImpl.touchModeChanged(false)
        // Ensure that recents view has focus so that it receives the followup key inputs
        if (requestFocus(recentsView.getTaskViewAt(keyboardTaskFocusIndex))) return
        if (requestFocus(recentsView.nextTaskView)) return
        if (requestFocus(recentsView.getTaskViewAt(0))) return
        requestFocus(recentsView)
    }

    private fun onRecentsViewFocusUpdated(command: CommandInfo) {
        val recentsView: RecentsView<*, *> =
            overviewComponentObserver.activityInterface.getVisibleRecentsView() ?: return
        if (command.type != TYPE_HIDE || keyboardTaskFocusIndex == PagedView.INVALID_PAGE) {
            return
        }
        recentsView.setKeyboardTaskFocusIndex(PagedView.INVALID_PAGE)
        recentsView.currentPage = keyboardTaskFocusIndex
        keyboardTaskFocusIndex = PagedView.INVALID_PAGE
    }

    private fun requestFocus(taskView: View?): Boolean {
        if (taskView == null) return false
        taskView.post {
            taskView.requestFocus()
            taskView.requestAccessibilityFocus()
        }
        return true
    }

    private fun logShowOverviewFrom(commandType: Int) {
        val activityInterface: BaseActivityInterface<*, *> =
            overviewComponentObserver.activityInterface
        val container = activityInterface.getCreatedContainer() as? RecentsViewContainer ?: return
        val event =
            when (commandType) {
                TYPE_SHOW -> LAUNCHER_OVERVIEW_SHOW_OVERVIEW_FROM_KEYBOARD_SHORTCUT
                TYPE_HIDE -> LAUNCHER_OVERVIEW_SHOW_OVERVIEW_FROM_KEYBOARD_QUICK_SWITCH
                TYPE_TOGGLE -> LAUNCHER_OVERVIEW_SHOW_OVERVIEW_FROM_3_BUTTON
                else -> return
            }
        StatsLogManager.newInstance(container.asContext())
            .logger()
            .withContainerInfo(
                LauncherAtom.ContainerInfo.newBuilder()
                    .setTaskSwitcherContainer(
                        LauncherAtom.TaskSwitcherContainer.getDefaultInstance()
                    )
                    .build()
            )
            .log(event)
    }

    fun dump(pw: PrintWriter) {
        pw.println("OverviewCommandHelper:")
        pw.println("  pendingCommands=${pendingCommands.size}")
        if (pendingCommands.isNotEmpty()) {
            pw.println("    pendingCommandType=${pendingCommands.first().type}")
        }
        pw.println("  mKeyboardTaskFocusIndex=$keyboardTaskFocusIndex")
        pw.println("  mWaitForToggleCommandComplete=$waitForToggleCommandComplete")
    }

    private data class CommandInfo(
        val type: Int,
        val createTime: Long = SystemClock.elapsedRealtime(),
        private var animationCallbacks: RecentsAnimationCallbacks? = null
    ) {
        fun setAnimationCallbacks(recentsAnimationCallbacks: RecentsAnimationCallbacks) {
            this.animationCallbacks = recentsAnimationCallbacks
        }

        fun addListener(listener: RecentsAnimationCallbacks.RecentsAnimationListener) {
            animationCallbacks?.addListener(listener)
        }

        fun removeListener(listener: RecentsAnimationCallbacks.RecentsAnimationListener?) {
            animationCallbacks?.removeListener(listener)
        }
    }

    companion object {
        private const val TAG = "OverviewCommandHelper"

        const val TYPE_SHOW: Int = 1
        const val TYPE_KEYBOARD_INPUT: Int = 2
        const val TYPE_HIDE: Int = 3
        const val TYPE_TOGGLE: Int = 4
        const val TYPE_HOME: Int = 5

        /**
         * Use case for needing a queue is double tapping recents button in 3 button nav. Size of 2
         * should be enough. We'll toss in one more because we're kind hearted.
         */
        private const val MAX_QUEUE_SIZE = 3

        private const val TRANSITION_NAME = "Transition:toOverview"
    }
}
