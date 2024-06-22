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
package com.android.launcher3.taskbar

import androidx.annotation.VisibleForTesting
import com.android.launcher3.Flags.enableRecentsInTaskbar
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.statehandlers.DesktopVisibilityController
import com.android.launcher3.taskbar.TaskbarControllers.LoggableTaskbarController
import com.android.launcher3.util.CancellableTask
import com.android.quickstep.RecentsModel
import com.android.quickstep.util.DesktopTask
import com.android.quickstep.util.GroupTask
import com.android.systemui.shared.recents.model.Task
import com.android.window.flags.Flags.enableDesktopWindowingMode
import com.android.window.flags.Flags.enableDesktopWindowingTaskbarRunningApps
import java.io.PrintWriter
import java.util.function.Consumer

/**
 * Provides recent apps functionality, when the Taskbar Recent Apps section is enabled. Behavior:
 * - When in Fullscreen mode: show the N most recent Tasks
 * - When in Desktop Mode: show the currently running (open) Tasks
 */
class TaskbarRecentAppsController(
    private val recentsModel: RecentsModel,
    // Pass a provider here instead of the actual DesktopVisibilityController instance since that
    // instance might not be available when this constructor is called.
    private val desktopVisibilityControllerProvider: () -> DesktopVisibilityController?,
) : LoggableTaskbarController {

    // TODO(b/335401172): unify DesktopMode checks in Launcher.
    var canShowRunningApps =
        enableDesktopWindowingMode() && enableDesktopWindowingTaskbarRunningApps()
        @VisibleForTesting
        set(isEnabledFromTest) {
            field = isEnabledFromTest
        }

    // TODO(b/343532825): Add a setting to disable Recents even when the flag is on.
    var canShowRecentApps = enableRecentsInTaskbar()
        @VisibleForTesting
        set(isEnabledFromTest) {
            field = isEnabledFromTest
        }

    // Initialized in init.
    private lateinit var controllers: TaskbarControllers

    private var shownHotseatItems: List<ItemInfo> = emptyList()
    private var allRecentTasks: List<GroupTask> = emptyList()
    private var desktopTask: DesktopTask? = null
    var shownTasks: List<GroupTask> = emptyList()
        private set

    private val desktopVisibilityController: DesktopVisibilityController?
        get() = desktopVisibilityControllerProvider()

    private val isInDesktopMode: Boolean
        get() = desktopVisibilityController?.areDesktopTasksVisible() ?: false

    val runningAppPackages: Set<String>
        /**
         * Returns the package names of apps that should be indicated as "running" to the user.
         * Specifically, we return all the open tasks if we are in Desktop mode, else emptySet().
         */
        get() {
            if (!canShowRunningApps || !isInDesktopMode) {
                return emptySet()
            }
            val tasks = desktopTask?.tasks ?: return emptySet()
            return tasks.map { task -> task.key.packageName }.toSet()
        }

    val minimizedAppPackages: Set<String>
        /**
         * Returns the package names of apps that should be indicated as "minimized" to the user.
         * Specifically, we return all the running packages where all the tasks in that package are
         * minimized (not visible).
         */
        get() {
            if (!canShowRunningApps || !isInDesktopMode) {
                return emptySet()
            }
            val desktopTasks = desktopTask?.tasks ?: return emptySet()
            val packageToTasks = desktopTasks.groupBy { it.key.packageName }
            return packageToTasks.filterValues { tasks -> tasks.all { !it.isVisible } }.keys
        }

    private val recentTasksChangedListener =
        RecentsModel.RecentTasksChangedListener { reloadRecentTasksIfNeeded() }

    private val iconLoadRequests: MutableSet<CancellableTask<*>> = HashSet()

    // TODO(b/343291428): add TaskVisualsChangListener as well (for calendar/clock?)

    // Used to keep track of the last requested task list ID, so that we do not request to load the
    // tasks again if we have already requested it and the task list has not changed
    private var taskListChangeId = -1

    fun init(taskbarControllers: TaskbarControllers) {
        controllers = taskbarControllers
        recentsModel.registerRecentTasksChangedListener(recentTasksChangedListener)
        reloadRecentTasksIfNeeded()
    }

    fun onDestroy() {
        recentsModel.unregisterRecentTasksChangedListener()
        iconLoadRequests.forEach { it.cancel() }
        iconLoadRequests.clear()
    }

    /** Called to update hotseatItems, in order to de-dupe them from Recent/Running tasks later. */
    fun updateHotseatItemInfos(hotseatItems: Array<ItemInfo?>): Array<ItemInfo?> {
        // Ignore predicted apps - we show running or recent apps instead.
        val removePredictions =
            (isInDesktopMode && canShowRunningApps) || (!isInDesktopMode && canShowRecentApps)
        if (!removePredictions) {
            shownHotseatItems = hotseatItems.filterNotNull()
            onRecentsOrHotseatChanged()
            return hotseatItems
        }
        shownHotseatItems =
            hotseatItems
                .filterNotNull()
                .filter { itemInfo -> !itemInfo.isPredictedItem }
                .toMutableList()

        onRecentsOrHotseatChanged()

        return shownHotseatItems.toTypedArray()
    }

    private fun reloadRecentTasksIfNeeded() {
        if (!recentsModel.isTaskListValid(taskListChangeId)) {
            taskListChangeId =
                recentsModel.getTasks { tasks ->
                    allRecentTasks = tasks
                    desktopTask = allRecentTasks.filterIsInstance<DesktopTask>().firstOrNull()
                    onRecentsOrHotseatChanged()
                    controllers.taskbarViewController.commitRunningAppsToUI()
                }
        }
    }

    private fun onRecentsOrHotseatChanged() {
        shownTasks =
            if (isInDesktopMode) {
                computeShownRunningTasks()
            } else {
                computeShownRecentTasks()
            }

        for (groupTask in shownTasks) {
            for (task in groupTask.tasks) {
                val callback =
                    Consumer<Task> { controllers.taskbarViewController.onTaskUpdated(it) }
                val cancellableTask = recentsModel.iconCache.updateIconInBackground(task, callback)
                if (cancellableTask != null) {
                    iconLoadRequests.add(cancellableTask)
                }
            }
        }
    }

    private fun computeShownRunningTasks(): List<GroupTask> {
        if (!canShowRunningApps) {
            return emptyList()
        }
        val tasks = desktopTask?.tasks ?: emptyList()
        // Kind of hacky, we wrap each single task in the Desktop as a GroupTask.
        var desktopTaskAsList = tasks.map { GroupTask(it) }
        // TODO(b/315344726 Multi-instance support): dedupe Tasks of the same package too.
        desktopTaskAsList = dedupeHotseatTasks(desktopTaskAsList, shownHotseatItems)
        val desktopPackages = desktopTaskAsList.map { it.packageNames }
        // Remove any missing Tasks.
        val newShownTasks = shownTasks.filter { it.packageNames in desktopPackages }.toMutableList()
        val newShownPackages = newShownTasks.map { it.packageNames }
        // Add any new Tasks, maintaining the order from previous shownTasks.
        newShownTasks.addAll(desktopTaskAsList.filter { it.packageNames !in newShownPackages })
        return newShownTasks.toList()
    }

    private fun computeShownRecentTasks(): List<GroupTask> {
        if (!canShowRecentApps || allRecentTasks.isEmpty()) {
            return emptyList()
        }
        // Remove the current task.
        val allRecentTasks = allRecentTasks.subList(0, allRecentTasks.size - 1)
        // TODO(b/315344726 Multi-instance support): dedupe Tasks of the same package too
        var shownTasks = dedupeHotseatTasks(allRecentTasks, shownHotseatItems)
        if (shownTasks.size > MAX_RECENT_TASKS) {
            // Remove any tasks older than MAX_RECENT_TASKS.
            shownTasks = shownTasks.subList(shownTasks.size - MAX_RECENT_TASKS, shownTasks.size)
        }
        return shownTasks
    }

    private fun dedupeHotseatTasks(
        groupTasks: List<GroupTask>,
        shownHotseatItems: List<ItemInfo>
    ): List<GroupTask> {
        val hotseatPackages = shownHotseatItems.map { item -> item.targetPackage }
        return groupTasks.filter { groupTask ->
            groupTask.hasMultipleTasks() ||
                !hotseatPackages.contains(groupTask.task1.key.packageName)
        }
    }

    override fun dumpLogs(prefix: String, pw: PrintWriter) {
        pw.println("$prefix TaskbarRecentAppsController:")
        pw.println("$prefix\tcanShowRunningApps=$canShowRunningApps")
        pw.println("$prefix\tcanShowRecentApps=$canShowRecentApps")
        pw.println("$prefix\tshownHotseatItems=${shownHotseatItems.map{item->item.targetPackage}}")
        pw.println("$prefix\tallRecentTasks=${allRecentTasks.map { it.packageNames }}")
        pw.println("$prefix\tdesktopTask=${desktopTask?.packageNames}")
        pw.println("$prefix\tshownTasks=${shownTasks.map { it.packageNames }}")
        pw.println("$prefix\trunningTasks=$runningAppPackages")
        pw.println("$prefix\tminimizedTasks=$minimizedAppPackages")
    }

    private val GroupTask.packageNames: List<String>
        get() = tasks.map { task -> task.key.packageName }

    private companion object {
        const val MAX_RECENT_TASKS = 2
    }
}
