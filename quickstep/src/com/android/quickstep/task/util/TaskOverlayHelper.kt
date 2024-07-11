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

package com.android.quickstep.task.util

import android.util.Log
import com.android.quickstep.TaskOverlayFactory
import com.android.quickstep.task.thumbnail.TaskOverlayUiState
import com.android.quickstep.task.thumbnail.TaskOverlayUiState.Disabled
import com.android.quickstep.task.thumbnail.TaskOverlayUiState.Enabled
import com.android.quickstep.task.viewmodel.TaskOverlayViewModel
import com.android.quickstep.views.RecentsView
import com.android.quickstep.views.RecentsViewContainer
import com.android.systemui.shared.recents.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Helper for [TaskOverlayFactory.TaskOverlay] to interact with [TaskOverlayViewModel], this helper
 * should merge with [TaskOverlayFactory.TaskOverlay] when it's migrated to MVVM.
 */
class TaskOverlayHelper(val task: Task, val overlay: TaskOverlayFactory.TaskOverlay<*>) {
    private lateinit var job: Job
    private var uiState: TaskOverlayUiState = Disabled

    // TODO(b/335649589): Ideally create and obtain this from DI. This ViewModel should be scoped
    //  to [TaskView], and also shared between [TaskView] and [TaskThumbnailView]
    //  This is using a lazy for now because the dependencies cannot be obtained without DI.
    private val taskOverlayViewModel by lazy {
        val recentsView =
            RecentsViewContainer.containerFromContext<RecentsViewContainer>(
                    overlay.taskView.context
                )
                .getOverviewPanel<RecentsView<*, *>>()
        TaskOverlayViewModel(task, recentsView.mRecentsViewData!!, recentsView.mTasksRepository!!)
    }

    // TODO(b/331753115): TaskOverlay should listen for state changes and react.
    val enabledState: Enabled
        get() = uiState as Enabled

    fun init() {
        // TODO(b/335396935): This should be changed to TaskView's scope.
        job =
            MainScope().launch {
                taskOverlayViewModel.overlayState.collect {
                    uiState = it
                    if (it is Enabled) {
                        Log.d(
                            TAG,
                            "initOverlay - taskId: ${task.key.id}, thumbnail: ${it.thumbnail}"
                        )
                        overlay.initOverlay(
                            task,
                            it.thumbnail,
                            it.thumbnailMatrix,
                            /* rotated= */ false
                        )
                    } else {
                        Log.d(TAG, "reset - taskId: ${task.key.id}")
                        overlay.reset()
                    }
                }
            }
    }

    fun destroy() {
        job.cancel()
        uiState = Disabled
        overlay.reset()
    }

    companion object {
        private const val TAG = "TaskOverlayHelper"
    }
}
