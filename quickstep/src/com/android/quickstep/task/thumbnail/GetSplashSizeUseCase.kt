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

package com.android.quickstep.task.thumbnail

import android.graphics.Point
import android.graphics.drawable.Drawable
import com.android.quickstep.recents.data.RecentsDeviceProfileRepository
import com.android.quickstep.task.viewmodel.TaskViewData
import kotlin.math.min

class GetSplashSizeUseCase(
    private val taskThumbnailViewData: TaskThumbnailViewData,
    private val taskViewData: TaskViewData,
    private val deviceProfileRepository: RecentsDeviceProfileRepository,
) {
    fun execute(splashImage: Drawable): Point {
        val recentsDeviceProfile = deviceProfileRepository.getRecentsDeviceProfile()
        val screenWidth = recentsDeviceProfile.widthPx
        val screenHeight = recentsDeviceProfile.heightPx
        val scaleAtFullscreen =
            min(
                screenWidth / taskThumbnailViewData.width.value,
                screenHeight / taskThumbnailViewData.height.value,
            )
        val scaleFactor: Float = 1f / taskViewData.nonGridScale.value / scaleAtFullscreen
        return Point(
            (splashImage.intrinsicWidth * scaleFactor / taskThumbnailViewData.scaleX.value).toInt(),
            (splashImage.intrinsicHeight * scaleFactor / taskThumbnailViewData.scaleY.value)
                .toInt(),
        )
    }
}
