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

package com.android.launcher3.taskbar.rules

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.launcher3.util.DisplayController
import com.android.launcher3.util.LauncherMultivalentJUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(LauncherMultivalentJUnit::class)
class TaskbarPinningPreferenceRuleTest {
    private val context = TaskbarWindowSandboxContext.create(getInstrumentation().targetContext)

    private val preferenceRule = TaskbarPinningPreferenceRule(context)

    @Test
    fun testEnablePinning_verifyDisplayController() {
        onSetup {
            preferenceRule.isPinned = true
            preferenceRule.isPinnedInDesktopMode = false
            assertThat(DisplayController.isPinnedTaskbar(context)).isTrue()
        }
    }

    @Test
    fun testDisablePinning_verifyDisplayController() {
        onSetup {
            preferenceRule.isPinned = false
            preferenceRule.isPinnedInDesktopMode = false
            assertThat(DisplayController.isPinnedTaskbar(context)).isFalse()
        }
    }

    @Test
    fun testEnableDesktopPinning_verifyDisplayController() {
        onSetup {
            preferenceRule.isPinned = false
            preferenceRule.isPinnedInDesktopMode = true
            assertThat(DisplayController.isPinnedTaskbar(context)).isTrue()
        }
    }

    @Test
    fun testDisableDesktopPinning_verifyDisplayController() {
        onSetup {
            preferenceRule.isPinned = false
            preferenceRule.isPinnedInDesktopMode = false
            assertThat(DisplayController.isPinnedTaskbar(context)).isFalse()
        }
    }

    @Test
    fun testTearDown_afterTogglingPinnedPreference_preferenceReset() {
        val wasPinned = preferenceRule.isPinned
        onSetup { preferenceRule.isPinned = !preferenceRule.isPinned }
        assertThat(preferenceRule.isPinned).isEqualTo(wasPinned)
    }

    @Test
    fun testTearDown_afterTogglingDesktopPreference_preferenceReset() {
        val wasPinnedInDesktopMode = preferenceRule.isPinnedInDesktopMode
        onSetup { preferenceRule.isPinnedInDesktopMode = !preferenceRule.isPinnedInDesktopMode }
        assertThat(preferenceRule.isPinnedInDesktopMode).isEqualTo(wasPinnedInDesktopMode)
    }

    /** Executes [runTest] after the [preferenceRule] setup phase completes. */
    private fun onSetup(runTest: () -> Unit) {
        preferenceRule.apply(
            object : Statement() {
                override fun evaluate() = runTest()
            },
            DESCRIPTION,
        )
    }

    private companion object {
        private val DESCRIPTION =
            Description.createSuiteDescription(TaskbarPinningPreferenceRule::class.java)
    }
}
