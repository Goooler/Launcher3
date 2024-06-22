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

package com.android.launcher3.taskbar

import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import android.testing.AndroidTestingRunner
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.statehandlers.DesktopVisibilityController
import com.android.quickstep.RecentsModel
import com.android.quickstep.RecentsModel.RecentTasksChangedListener
import com.android.quickstep.TaskIconCache
import com.android.quickstep.util.DesktopTask
import com.android.quickstep.util.GroupTask
import com.android.systemui.shared.recents.model.Task
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidTestingRunner::class)
class TaskbarRecentAppsControllerTest : TaskbarBaseTestCase() {

    @get:Rule val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockIconCache: TaskIconCache
    @Mock private lateinit var mockRecentsModel: RecentsModel
    @Mock private lateinit var mockDesktopVisibilityController: DesktopVisibilityController

    private var nextTaskId: Int = 500
    private var taskListChangeId: Int = 1

    private lateinit var recentAppsController: TaskbarRecentAppsController
    private lateinit var recentTasksChangedListener: RecentTasksChangedListener
    private lateinit var userHandle: UserHandle

    @Before
    fun setUp() {
        super.setup()
        userHandle = Process.myUserHandle()

        whenever(mockRecentsModel.iconCache).thenReturn(mockIconCache)
        recentAppsController =
            TaskbarRecentAppsController(mockRecentsModel) { mockDesktopVisibilityController }
        recentAppsController.init(taskbarControllers)
        recentAppsController.canShowRunningApps = true
        recentAppsController.canShowRecentApps = true

        val listenerCaptor = ArgumentCaptor.forClass(RecentTasksChangedListener::class.java)
        verify(mockRecentsModel).registerRecentTasksChangedListener(listenerCaptor.capture())
        recentTasksChangedListener = listenerCaptor.value
    }

    @Test
    fun updateHotseatItemInfos_cantShowRunning_inDesktopMode_returnsAllHotseatItems() {
        recentAppsController.canShowRunningApps = false
        setInDesktopMode(true)
        val hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1)
        val newHotseatItems =
            prepareHotseatAndRunningAndRecentApps(
                hotseatPackages = hotseatPackages,
                runningTaskPackages = emptyList(),
                recentTaskPackages = emptyList()
            )
        assertThat(newHotseatItems.map { it?.targetPackage })
            .containsExactlyElementsIn(hotseatPackages)
    }

    @Test
    fun updateHotseatItemInfos_cantShowRecent_notInDesktopMode_returnsAllHotseatItems() {
        recentAppsController.canShowRecentApps = false
        setInDesktopMode(false)
        val hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1)
        val newHotseatItems =
            prepareHotseatAndRunningAndRecentApps(
                hotseatPackages = hotseatPackages,
                runningTaskPackages = emptyList(),
                recentTaskPackages = emptyList()
            )
        assertThat(newHotseatItems.map { it?.targetPackage })
            .containsExactlyElementsIn(hotseatPackages)
    }

    @Test
    fun updateHotseatItemInfos_canShowRunning_inDesktopMode_returnsNonPredictedHotseatItems() {
        recentAppsController.canShowRunningApps = true
        setInDesktopMode(true)
        val newHotseatItems =
            prepareHotseatAndRunningAndRecentApps(
                hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1),
                runningTaskPackages = emptyList(),
                recentTaskPackages = emptyList()
            )
        val expectedPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2)
        assertThat(newHotseatItems.map { it?.targetPackage })
            .containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun updateHotseatItemInfos_canShowRecent_notInDesktopMode_returnsNonPredictedHotseatItems() {
        recentAppsController.canShowRecentApps = true
        setInDesktopMode(false)
        val newHotseatItems =
            prepareHotseatAndRunningAndRecentApps(
                hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1),
                runningTaskPackages = emptyList(),
                recentTaskPackages = emptyList()
            )
        val expectedPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2)
        assertThat(newHotseatItems.map { it?.targetPackage })
            .containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun onRecentTasksChanged_cantShowRunning_inDesktopMode_shownTasks_returnsEmptyList() {
        recentAppsController.canShowRunningApps = false
        setInDesktopMode(true)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1),
            runningTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2),
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.shownTasks).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_cantShowRecent_notInDesktopMode_shownTasks_returnsEmptyList() {
        recentAppsController.canShowRecentApps = false
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2, PREDICTED_PACKAGE_1),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        )
        assertThat(recentAppsController.shownTasks).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_noRecentTasks_shownTasks_returnsEmptyList() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2),
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.shownTasks).isEmpty()
        assertThat(recentAppsController.minimizedAppPackages).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_noRunningApps_shownTasks_returnsEmptyList() {
        setInDesktopMode(true)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        )
        assertThat(recentAppsController.shownTasks).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_shownTasks_returnsRunningTasks() {
        setInDesktopMode(true)
        val runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            recentTaskPackages = emptyList()
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        assertThat(shownPackages).containsExactlyElementsIn(runningTaskPackages)
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_runningAppIsHotseatItem_shownTasks_returnsDistinctItems() {
        setInDesktopMode(true)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2),
            runningTaskPackages =
                listOf(HOTSEAT_PACKAGE_1, RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2),
            recentTaskPackages = emptyList()
        )
        val expectedPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        assertThat(shownPackages).containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_getRunningApps_returnsEmptySet() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2),
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.runningAppPackages).isEmpty()
        assertThat(recentAppsController.minimizedAppPackages).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_getRunningApps_returnsAllDesktopTasks() {
        setInDesktopMode(true)
        val runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.runningAppPackages)
            .containsExactlyElementsIn(runningTaskPackages)
        assertThat(recentAppsController.minimizedAppPackages).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_getRunningApps_includesHotseat() {
        setInDesktopMode(true)
        val runningTaskPackages =
            listOf(HOTSEAT_PACKAGE_1, RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = listOf(HOTSEAT_PACKAGE_1, HOTSEAT_PACKAGE_2),
            runningTaskPackages = runningTaskPackages,
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        )
        assertThat(recentAppsController.runningAppPackages)
            .containsExactlyElementsIn(runningTaskPackages)
        assertThat(recentAppsController.minimizedAppPackages).isEmpty()
    }

    @Test
    fun getMinimizedApps_inDesktopMode_returnsAllAppsRunningAndInvisibleAppsMinimized() {
        setInDesktopMode(true)
        val runningTaskPackages =
            listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_3)
        val minimizedTaskIndices = setOf(2) // RUNNING_APP_PACKAGE_3
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            minimizedTaskIndices = minimizedTaskIndices,
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.runningAppPackages)
            .containsExactlyElementsIn(runningTaskPackages)
        assertThat(recentAppsController.minimizedAppPackages).containsExactly(RUNNING_APP_PACKAGE_3)
    }

    @Test
    fun getMinimizedApps_inDesktopMode_twoTasksSamePackageOneMinimizedReturnsNotMinimized() {
        setInDesktopMode(true)
        val runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_1)
        val minimizedTaskIndices = setOf(1) // The second RUNNING_APP_PACKAGE_1 task.
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            minimizedTaskIndices = minimizedTaskIndices,
            recentTaskPackages = emptyList()
        )
        assertThat(recentAppsController.runningAppPackages)
            .containsExactlyElementsIn(runningTaskPackages.toSet())
        assertThat(recentAppsController.minimizedAppPackages).isEmpty()
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_shownTasks_maintainsOrder() {
        setInDesktopMode(true)
        val originalOrder = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = originalOrder,
            recentTaskPackages = emptyList()
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_1),
            recentTaskPackages = emptyList()
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        assertThat(shownPackages).isEqualTo(originalOrder)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_shownTasks_maintainsRecency() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2, RECENT_PACKAGE_3)
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_2, RECENT_PACKAGE_3, RECENT_PACKAGE_1)
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        // Most recent packages, minus the currently running one (RECENT_PACKAGE_1).
        assertThat(shownPackages).isEqualTo(listOf(RECENT_PACKAGE_2, RECENT_PACKAGE_3))
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_addTask_shownTasks_maintainsOrder() {
        setInDesktopMode(true)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2),
            recentTaskPackages = emptyList()
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages =
                listOf(RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_3),
            recentTaskPackages = emptyList()
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        val expectedOrder =
            listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_3)
        assertThat(shownPackages).isEqualTo(expectedOrder)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_addTask_shownTasks_maintainsRecency() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_3, RECENT_PACKAGE_2)
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_2, RECENT_PACKAGE_3, RECENT_PACKAGE_1)
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        // Most recent packages, minus the currently running one (RECENT_PACKAGE_1).
        assertThat(shownPackages).isEqualTo(listOf(RECENT_PACKAGE_2, RECENT_PACKAGE_3))
    }

    @Test
    fun onRecentTasksChanged_inDesktopMode_removeTask_shownTasks_maintainsOrder() {
        setInDesktopMode(true)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages =
                listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_3),
            recentTaskPackages = emptyList()
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_2, RUNNING_APP_PACKAGE_1),
            recentTaskPackages = emptyList()
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        assertThat(shownPackages).isEqualTo(listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2))
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_removeTask_shownTasks_maintainsRecency() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2, RECENT_PACKAGE_3)
        )
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_2, RECENT_PACKAGE_3)
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        // Most recent packages, minus the currently running one (RECENT_PACKAGE_3).
        assertThat(shownPackages).isEqualTo(listOf(RECENT_PACKAGE_2))
    }

    @Test
    fun onRecentTasksChanged_enterDesktopMode_shownTasks_onlyIncludesRunningTasks() {
        setInDesktopMode(false)
        val runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        val recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            recentTaskPackages = recentTaskPackages
        )
        setInDesktopMode(true)
        recentTasksChangedListener.onRecentTasksChanged()
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        assertThat(shownPackages).containsExactlyElementsIn(runningTaskPackages)
    }

    @Test
    fun onRecentTasksChanged_exitDesktopMode_shownTasks_onlyIncludesRecentTasks() {
        setInDesktopMode(true)
        val runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        val recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2, RECENT_PACKAGE_3)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = runningTaskPackages,
            recentTaskPackages = recentTaskPackages
        )
        setInDesktopMode(false)
        recentTasksChangedListener.onRecentTasksChanged()
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        // Don't expect RECENT_PACKAGE_3 because it is currently running.
        val expectedPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        assertThat(shownPackages).containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_hasRecentTasks_shownTasks_returnsRecentTasks() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2, RECENT_PACKAGE_3)
        )
        val shownPackages = recentAppsController.shownTasks.flatMap { it.packageNames }
        // RECENT_PACKAGE_3 is the top task (visible to user) so should be excluded.
        val expectedPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        assertThat(shownPackages).containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_hasRecentAndRunningTasks_shownTasks_returnsRecentTaskAndDesktopTile() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2),
            recentTaskPackages = listOf(RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        )
        val shownPackages = recentAppsController.shownTasks.map { it.packageNames }
        // Only 2 recent tasks shown: Desktop Tile + 1 Recent Task
        val desktopTilePackages = listOf(RUNNING_APP_PACKAGE_1, RUNNING_APP_PACKAGE_2)
        val recentTaskPackages = listOf(RECENT_PACKAGE_1)
        val expectedPackages = listOf(desktopTilePackages, recentTaskPackages)
        assertThat(shownPackages).containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun onRecentTasksChanged_notInDesktopMode_hasRecentAndSplitTasks_shownTasks_returnsRecentTaskAndPair() {
        setInDesktopMode(false)
        prepareHotseatAndRunningAndRecentApps(
            hotseatPackages = emptyList(),
            runningTaskPackages = emptyList(),
            recentTaskPackages = listOf(RECENT_SPLIT_PACKAGES_1, RECENT_PACKAGE_1, RECENT_PACKAGE_2)
        )
        val shownPackages = recentAppsController.shownTasks.map { it.packageNames }
        // Only 2 recent tasks shown: Pair + 1 Recent Task
        val pairPackages = RECENT_SPLIT_PACKAGES_1.split("_")
        val recentTaskPackages = listOf(RECENT_PACKAGE_1)
        val expectedPackages = listOf(pairPackages, recentTaskPackages)
        assertThat(shownPackages).containsExactlyElementsIn(expectedPackages)
    }

    private fun prepareHotseatAndRunningAndRecentApps(
        hotseatPackages: List<String>,
        runningTaskPackages: List<String>,
        minimizedTaskIndices: Set<Int> = emptySet(),
        recentTaskPackages: List<String>,
    ): Array<ItemInfo?> {
        val hotseatItems = createHotseatItemsFromPackageNames(hotseatPackages)
        val newHotseatItems =
            recentAppsController.updateHotseatItemInfos(hotseatItems.toTypedArray())
        val runningTasks = createDesktopTask(runningTaskPackages, minimizedTaskIndices)
        val recentTasks = createRecentTasksFromPackageNames(recentTaskPackages)
        val allTasks =
            ArrayList<GroupTask>().apply {
                if (runningTasks != null) {
                    add(runningTasks)
                }
                addAll(recentTasks)
            }
        doAnswer {
                val callback: Consumer<ArrayList<GroupTask>> = it.getArgument(0)
                callback.accept(allTasks)
                taskListChangeId
            }
            .whenever(mockRecentsModel)
            .getTasks(any<Consumer<List<GroupTask>>>())
        recentTasksChangedListener.onRecentTasksChanged()
        return newHotseatItems
    }

    private fun createHotseatItemsFromPackageNames(packageNames: List<String>): List<ItemInfo> {
        return packageNames.map {
            createTestAppInfo(packageName = it).apply {
                container =
                    if (it.startsWith("predicted")) {
                        CONTAINER_HOTSEAT_PREDICTION
                    } else {
                        CONTAINER_HOTSEAT
                    }
            }
        }
    }

    private fun createTestAppInfo(
        packageName: String = "testPackageName",
        className: String = "testClassName"
    ) = AppInfo(ComponentName(packageName, className), className /* title */, userHandle, Intent())

    private fun createDesktopTask(
        packageNames: List<String>,
        minimizedTaskIndices: Set<Int>
    ): DesktopTask? {
        if (packageNames.isEmpty()) return null

        return DesktopTask(
            ArrayList(
                packageNames.mapIndexed { index, packageName ->
                    createTask(packageName, index !in minimizedTaskIndices)
                }
            )
        )
    }

    private fun createRecentTasksFromPackageNames(packageNames: List<String>): List<GroupTask> {
        return packageNames.map {
            if (it.startsWith("split")) {
                val splitPackages = it.split("_")
                GroupTask(
                    createTask(splitPackages[0]),
                    createTask(splitPackages[1]),
                    /* splitBounds = */ null
                )
            } else {
                GroupTask(createTask(it))
            }
        }
    }

    private fun createTask(packageName: String, isVisible: Boolean = true): Task {
        return Task(
                Task.TaskKey(
                    nextTaskId++,
                    WINDOWING_MODE_FREEFORM,
                    Intent().apply { `package` = packageName },
                    ComponentName(packageName, "TestActivity"),
                    userHandle.identifier,
                    0
                )
            )
            .apply { this.isVisible = isVisible }
    }

    private fun setInDesktopMode(inDesktopMode: Boolean) {
        whenever(mockDesktopVisibilityController.areDesktopTasksVisible()).thenReturn(inDesktopMode)
    }

    private val GroupTask.packageNames: List<String>
        get() = tasks.map { task -> task.key.packageName }

    private companion object {
        const val HOTSEAT_PACKAGE_1 = "hotseat1"
        const val HOTSEAT_PACKAGE_2 = "hotseat2"
        const val PREDICTED_PACKAGE_1 = "predicted1"
        const val RUNNING_APP_PACKAGE_1 = "running1"
        const val RUNNING_APP_PACKAGE_2 = "running2"
        const val RUNNING_APP_PACKAGE_3 = "running3"
        const val RECENT_PACKAGE_1 = "recent1"
        const val RECENT_PACKAGE_2 = "recent2"
        const val RECENT_PACKAGE_3 = "recent3"
        const val RECENT_SPLIT_PACKAGES_1 = "split1_split2"
    }
}
