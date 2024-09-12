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

package com.android.quickstep.util

import com.android.launcher3.Flags.enableLargeDesktopWindowingTile
import com.android.quickstep.RecentsAnimationController
import com.android.quickstep.views.DesktopTaskView
import com.android.quickstep.views.TaskView
import com.android.quickstep.views.TaskViewType
import com.android.systemui.shared.recents.model.ThumbnailData

/**
 * Helper class for [com.android.quickstep.views.RecentsView]. This util class contains refactored
 * and extracted functions from RecentsView to facilitate the implementation of unit tests.
 */
class RecentsViewUtils {

    /**
     * Sort task groups to move desktop tasks to the end of the list.
     *
     * @param tasks List of group tasks to be sorted.
     * @return Sorted list of GroupTasks to be used in the RecentsView.
     */
    fun sortDesktopTasksToFront(tasks: List<GroupTask>): List<GroupTask> {
        val (desktopTasks, otherTasks) = tasks.partition { it.taskViewType == TaskViewType.DESKTOP }
        return otherTasks + desktopTasks
    }

    fun getFocusedTaskIndex(taskGroups: List<GroupTask>): Int {
        // The focused task index is placed after the desktop tasks views.
        return if (enableLargeDesktopWindowingTile()) {
            taskGroups.count { it.taskViewType == TaskViewType.DESKTOP }
        } else {
            0
        }
    }

    /**
     * Counts [TaskView]s that are [DesktopTaskView] instances.
     *
     * @param taskViews List of [TaskView]s
     */
    fun getDesktopTaskViewCount(taskViews: List<TaskView>): Int =
        taskViews.count { it is DesktopTaskView }

    /** Returns a list of all large TaskView Ids from [TaskView]s */
    fun getLargeTaskViewIds(taskViews: Iterable<TaskView>): List<Int> =
        taskViews.filter { it.isLargeTile }.map { it.taskViewId }

    /**
     * Returns the first TaskView that should be displayed as a large tile.
     *
     * @param taskViews List of [TaskView]s
     */
    fun getFirstLargeTaskView(taskViews: List<TaskView>): TaskView? =
        taskViews.firstOrNull { it.isLargeTile }

    fun screenshotTasks(
        taskView: TaskView,
        recentsAnimationController: RecentsAnimationController,
    ): Map<Int, ThumbnailData> =
        taskView.taskContainers.associate {
            it.task.key.id to recentsAnimationController.screenshotTask(it.task.key.id)
        }

    /** Returns the current list of [TaskView] children. */
    fun getTaskViews(taskViewCount: Int, requireTaskViewAt: (Int) -> TaskView): List<TaskView> =
        (0 until taskViewCount).map(requireTaskViewAt)
}
