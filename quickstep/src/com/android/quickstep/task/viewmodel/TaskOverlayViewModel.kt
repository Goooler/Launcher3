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

package com.android.quickstep.task.viewmodel

import android.graphics.Matrix
import com.android.quickstep.recents.data.RecentTasksRepository
import com.android.quickstep.recents.viewmodel.RecentsViewData
import com.android.quickstep.task.thumbnail.TaskOverlayUiState.Disabled
import com.android.quickstep.task.thumbnail.TaskOverlayUiState.Enabled
import com.android.systemui.shared.recents.model.Task
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

/** View model for TaskOverlay */
class TaskOverlayViewModel(
    task: Task,
    recentsViewData: RecentsViewData,
    tasksRepository: RecentTasksRepository,
) {
    val overlayState =
        combine(
                recentsViewData.overlayEnabled,
                recentsViewData.settledFullyVisibleTaskIds.map { it.contains(task.key.id) },
                tasksRepository
                    .getTaskDataById(task.key.id)
                    .map { it?.thumbnail }
                    .distinctUntilChangedBy { it?.snapshotId }
            ) { isOverlayEnabled, isFullyVisible, thumbnailData ->
                if (isOverlayEnabled && isFullyVisible) {
                    Enabled(
                        isRealSnapshot = (thumbnailData?.isRealSnapshot ?: false) && !task.isLocked,
                        thumbnailData?.thumbnail,
                        // TODO(b/343101424): Use PreviewPositionHelper, listen from a common source
                        // with
                        //  TaskThumbnailView.
                        Matrix.IDENTITY_MATRIX
                    )
                } else {
                    Disabled
                }
            }
            .distinctUntilChanged()
}
