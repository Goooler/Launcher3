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
import com.android.launcher3.ConstantItem
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherPrefs.Companion.TASKBAR_PINNING
import com.android.launcher3.LauncherPrefs.Companion.TASKBAR_PINNING_IN_DESKTOP_MODE
import kotlin.reflect.KProperty
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule that allows modifying the Taskbar pinned preferences.
 *
 * The original preference values are restored on teardown.
 *
 * If this rule is being used with [TaskbarUnitTestRule], make sure this rule is applied first.
 *
 * This rule is overkill if a test does not need to change the mode during Taskbar's lifecycle. If
 * the mode is static, use [TaskbarModeRule] instead, which forces the mode. A test can class can
 * declare both this rule and [TaskbarModeRule] but using both for a test method is unsupported.
 */
class TaskbarPinningPreferenceRule(context: TaskbarWindowSandboxContext) : TestRule {

    private val prefs = LauncherPrefs.get(context)

    var isPinned by PinningPreference(TASKBAR_PINNING)
    var isPinnedInDesktopMode by PinningPreference(TASKBAR_PINNING_IN_DESKTOP_MODE)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val wasPinned = isPinned
                val wasPinnedInDesktopMode = isPinnedInDesktopMode
                try {
                    base.evaluate()
                } finally {
                    isPinned = wasPinned
                    isPinnedInDesktopMode = wasPinnedInDesktopMode
                }
            }
        }
    }

    private inner class PinningPreference(private val constantItem: ConstantItem<Boolean>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            return prefs.get(constantItem)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            getInstrumentation().runOnMainSync { prefs.put(constantItem, value) }
        }
    }
}
