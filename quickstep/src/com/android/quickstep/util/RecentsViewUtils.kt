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
import com.android.quickstep.views.DesktopTaskView
import com.android.quickstep.views.TaskView
import com.android.quickstep.views.TaskViewType

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
     * Counts [numChildren] that are [DesktopTaskView] instances.
     *
     * @param numChildren Quantity of children to transverse
     * @param getTaskViewAt Function that provides a TaskView given an index
     */
    fun getDesktopTaskViewCount(numChildren: Int, getTaskViewAt: (Int) -> TaskView?): Int =
        (0 until numChildren).count { getTaskViewAt(it) is DesktopTaskView }

    /**
     * Returns the first TaskView that should be displayed as a large tile.
     *
     * @param numChildren Quantity of children to transverse
     * @param getTaskViewAt Function that provides a TaskView given an index
     */
    fun getFirstLargeTaskView(numChildren: Int, getTaskViewAt: (Int) -> TaskView?): TaskView? {
        return (0 until numChildren).firstNotNullOfOrNull { index ->
            val taskView = getTaskViewAt(index)
            if (taskView?.isLargeTile == true) taskView else null
        }
    }
}
