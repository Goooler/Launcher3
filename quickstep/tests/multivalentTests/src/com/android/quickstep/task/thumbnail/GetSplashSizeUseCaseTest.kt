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
import com.android.quickstep.recents.data.FakeRecentsDeviceProfileRepository
import com.android.quickstep.task.viewmodel.TaskViewData
import com.android.quickstep.views.TaskViewType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetSplashSizeUseCaseTest {
    private val taskThumbnailViewData = TaskThumbnailViewData()
    private val taskViewData = TaskViewData(TaskViewType.SINGLE)
    private val recentsDeviceProfileRepository = FakeRecentsDeviceProfileRepository()
    private val systemUnderTest =
        GetSplashSizeUseCase(taskThumbnailViewData, taskViewData, recentsDeviceProfileRepository)

    @Test
    fun execute_whenNoScaleRequired_returnsIntrinsicSize() {
        taskThumbnailViewData.width.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().widthPx
        taskThumbnailViewData.height.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().heightPx

        assertThat(systemUnderTest.execute(createIcon(100, 100))).isEqualTo(Point(100, 100))
    }

    @Test
    fun execute_whenThumbnailViewIsSmallerThanScreen_returnsScaledSize() {
        taskThumbnailViewData.width.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().widthPx / 2
        taskThumbnailViewData.height.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().heightPx / 2

        assertThat(systemUnderTest.execute(createIcon(100, 100))).isEqualTo(Point(50, 50))
    }

    @Test
    fun execute_whenThumbnailViewIsSmallerThanScreen_withNonGridScale_returnsScaledSize() {
        taskThumbnailViewData.width.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().widthPx / 2
        taskThumbnailViewData.height.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().heightPx / 2
        taskViewData.nonGridScale.value = 2f

        assertThat(systemUnderTest.execute(createIcon(100, 100))).isEqualTo(Point(25, 25))
    }

    @Test
    fun execute_whenThumbnailViewIsSmallerThanScreen_withThumbnailViewScale_returnsScaledSize() {
        taskThumbnailViewData.width.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().widthPx / 2
        taskThumbnailViewData.height.value =
            recentsDeviceProfileRepository.getRecentsDeviceProfile().heightPx / 2
        taskThumbnailViewData.scaleX.value = 2f
        taskThumbnailViewData.scaleY.value = 2f

        assertThat(systemUnderTest.execute(createIcon(100, 100))).isEqualTo(Point(25, 25))
    }

    private fun createIcon(width: Int, height: Int): Drawable =
        mock<Drawable>().apply {
            whenever(intrinsicWidth).thenReturn(width)
            whenever(intrinsicHeight).thenReturn(height)
        }
}
