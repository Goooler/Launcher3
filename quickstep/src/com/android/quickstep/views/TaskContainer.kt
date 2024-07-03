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

package com.android.quickstep.views

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import com.android.launcher3.Flags.enableRefactorTaskThumbnail
import com.android.launcher3.Flags.privateSpaceRestrictAccessibilityDrag
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.data.ItemInfoWithIcon
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.util.SplitConfigurationOptions
import com.android.launcher3.util.TransformingTouchDelegate
import com.android.quickstep.TaskOverlayFactory
import com.android.quickstep.TaskUtils
import com.android.quickstep.task.thumbnail.TaskThumbnail
import com.android.quickstep.task.thumbnail.TaskThumbnailView
import com.android.quickstep.task.viewmodel.TaskContainerData
import com.android.systemui.shared.recents.model.Task

/** Holder for all Task dependent information. */
class TaskContainer(
    val taskView: TaskView,
    val task: Task,
    val thumbnailView: TaskThumbnailView?,
    val thumbnailViewDeprecated: TaskThumbnailViewDeprecated,
    val iconView: TaskViewIcon,
    /**
     * This technically can be a vanilla [android.view.TouchDelegate] class, however that class
     * requires setting the touch bounds at construction, so we'd repeatedly be created many
     * instances unnecessarily as scrolling occurs, whereas [TransformingTouchDelegate] allows touch
     * delegated bounds only to be updated.
     */
    val iconTouchDelegate: TransformingTouchDelegate,
    /** Defaults to STAGE_POSITION_UNDEFINED if in not a split screen task view */
    @SplitConfigurationOptions.StagePosition val stagePosition: Int,
    val digitalWellBeingToast: DigitalWellBeingToast?,
    val showWindowsView: View?,
    taskOverlayFactory: TaskOverlayFactory
) {
    val overlay: TaskOverlayFactory.TaskOverlay<*> = taskOverlayFactory.createOverlay(this)
    val taskContainerData = TaskContainerData()

    val snapshotView: View
        get() = thumbnailView ?: thumbnailViewDeprecated

    // TODO(b/349120849): Extract ThumbnailData from TaskContainerData/TaskThumbnailViewModel
    val thumbnail: Bitmap?
        get() = thumbnailViewDeprecated.thumbnail

    // TODO(b/334826842): Support shouldShowSplashView for new TTV.
    val shouldShowSplashView: Boolean
        get() =
            if (enableRefactorTaskThumbnail()) false
            else thumbnailViewDeprecated.shouldShowSplashView()

    // TODO(b/350743460) Support sysUiStatusNavFlags for new TTV.
    val sysUiStatusNavFlags: Int
        get() =
            if (enableRefactorTaskThumbnail()) 0 else thumbnailViewDeprecated.sysUiStatusNavFlags

    /** Builds proto for logging */
    val itemInfo: WorkspaceItemInfo
        get() =
            WorkspaceItemInfo().apply {
                itemType = LauncherSettings.Favorites.ITEM_TYPE_TASK
                container = LauncherSettings.Favorites.CONTAINER_TASKSWITCHER
                val componentKey = TaskUtils.getLaunchComponentKeyForTask(task.key)
                user = componentKey.user
                intent = Intent().setComponent(componentKey.componentName)
                title = task.title
                taskView.recentsView?.let { screenId = it.indexOfChild(taskView) }
                if (privateSpaceRestrictAccessibilityDrag()) {
                    if (
                        UserCache.getInstance(taskView.context)
                            .getUserInfo(componentKey.user)
                            .isPrivate
                    ) {
                        runtimeStatusFlags =
                            runtimeStatusFlags or ItemInfoWithIcon.FLAG_NOT_PINNABLE
                    }
                }
            }

    fun destroy() {
        digitalWellBeingToast?.destroy()
        thumbnailView?.let { taskView.removeView(it) }
        overlay.destroy()
    }

    fun bind() {
        if (enableRefactorTaskThumbnail() && thumbnailView != null) {
            thumbnailViewDeprecated.setTaskOverlay(overlay)
            bindThumbnailView()
            overlay.init()
        } else {
            thumbnailViewDeprecated.bind(task, overlay)
        }
    }

    // TODO(b/335649589): TaskView's VM will already have access to TaskThumbnailView's VM
    //  so there will be no need to access TaskThumbnailView's VM through the TaskThumbnailView
    fun bindThumbnailView() {
        // TODO(b/343364498): Existing view has shouldShowScreenshot as an override as well but
        //  this should be decided inside TaskThumbnailViewModel.
        thumbnailView?.viewModel?.bind(TaskThumbnail(task.key.id, taskView.isRunningTask))
    }

    fun setOverlayEnabled(enabled: Boolean) {
        if (!enableRefactorTaskThumbnail()) {
            thumbnailViewDeprecated.setOverlayEnabled(enabled)
        }
    }
}
