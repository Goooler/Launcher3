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

package com.android.quickstep.logging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherPrefs.Companion.backedUpItem
import com.android.launcher3.logging.InstanceId
import com.android.launcher3.logging.StatsLogManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SettingsChangeLoggerTest {
    private val mContext: Context = ApplicationProvider.getApplicationContext()

    private val mInstanceId = InstanceId.fakeInstanceId(1)

    private lateinit var mSystemUnderTest: SettingsChangeLogger

    @Mock private lateinit var mStatsLogManager: StatsLogManager

    @Mock private lateinit var mMockLogger: StatsLogManager.StatsLogger

    @Captor private lateinit var mEventCaptor: ArgumentCaptor<StatsLogManager.EventEnum>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        whenever(mStatsLogManager.logger()).doReturn(mMockLogger)
        whenever(mStatsLogManager.logger().withInstanceId(any())).doReturn(mMockLogger)

        mSystemUnderTest = SettingsChangeLogger(mContext, mStatsLogManager)
    }

    @After
    fun tearDown() {
        mSystemUnderTest.close()
    }

    @Test
    fun loggingPrefs_correctDefaultValue() {
        assertThat(mSystemUnderTest.loggingPrefs["pref_allowRotation"]!!.defaultValue).isFalse()
        assertThat(mSystemUnderTest.loggingPrefs["pref_add_icon_to_home"]!!.defaultValue).isTrue()
        assertThat(mSystemUnderTest.loggingPrefs["pref_overview_action_suggestions"]!!.defaultValue)
            .isTrue()
        assertThat(mSystemUnderTest.loggingPrefs["pref_smartspace_home_screen"]!!.defaultValue)
            .isTrue()
        assertThat(mSystemUnderTest.loggingPrefs["pref_enable_minus_one"]!!.defaultValue).isTrue()
    }

    @Test
    fun logSnapshot_defaultValue() {
        mSystemUnderTest.logSnapshot(mInstanceId)

        verify(mMockLogger, atLeastOnce()).log(mEventCaptor.capture())
        val capturedEvents = mEventCaptor.allValues
        assertThat(capturedEvents.isNotEmpty()).isTrue()
        verifyDefaultEvent(capturedEvents)
        // pref_allowRotation false
        assertThat(capturedEvents.any { it.id == 616 }).isTrue()
    }

    @Test
    fun logSnapshot_updateValue() {
        LauncherPrefs.get(mContext)
            .put(
                item =
                    backedUpItem(
                        sharedPrefKey = "pref_allowRotation",
                        defaultValue = false,
                    ),
                value = true
            )

        mSystemUnderTest.logSnapshot(mInstanceId)

        verify(mMockLogger, atLeastOnce()).log(mEventCaptor.capture())
        val capturedEvents = mEventCaptor.allValues
        assertThat(capturedEvents.isNotEmpty()).isTrue()
        verifyDefaultEvent(capturedEvents)
        // pref_allowRotation true
        assertThat(capturedEvents.any { it.id == 615 }).isTrue()
    }

    private fun verifyDefaultEvent(capturedEvents: MutableList<StatsLogManager.EventEnum>) {
        // LAUNCHER_NOTIFICATION_DOT_ENABLED
        assertThat(capturedEvents.any { it.id == 611 }).isTrue()
        // LAUNCHER_NAVIGATION_MODE_GESTURE_BUTTON
        assertThat(capturedEvents.any { it.id == 625 }).isTrue()
        // LAUNCHER_THEMED_ICON_DISABLED
        assertThat(capturedEvents.any { it.id == 837 }).isTrue()
        // pref_add_icon_to_home true
        assertThat(capturedEvents.any { it.id == 613 }).isTrue()
        // pref_overview_action_suggestions true
        assertThat(capturedEvents.any { it.id == 619 }).isTrue()
        // pref_smartspace_home_screen true
        assertThat(capturedEvents.any { it.id == 621 }).isTrue()
        // pref_enable_minus_one true
        assertThat(capturedEvents.any { it.id == 617 }).isTrue()
    }
}
