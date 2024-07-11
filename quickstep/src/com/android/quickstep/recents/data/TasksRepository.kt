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

package com.android.quickstep.recents.data

import android.graphics.drawable.Drawable
import com.android.quickstep.task.thumbnail.data.TaskIconDataSource
import com.android.quickstep.task.thumbnail.data.TaskThumbnailDataSource
import com.android.quickstep.util.GroupTask
import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.recents.model.ThumbnailData
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class TasksRepository(
    private val recentsModel: RecentTasksDataSource,
    private val taskThumbnailDataSource: TaskThumbnailDataSource,
    private val taskIconDataSource: TaskIconDataSource,
) : RecentTasksRepository {
    private val groupedTaskData = MutableStateFlow(emptyList<GroupTask>())
    private val _taskData =
        groupedTaskData.map { groupTaskList -> groupTaskList.flatMap { it.tasks } }
    private val visibleTaskIds = MutableStateFlow(emptySet<Int>())

    private val taskData: Flow<List<Task>> =
        combine(_taskData, getThumbnailQueryResults(), getIconQueryResults()) {
            tasks,
            thumbnailQueryResults,
            iconQueryResults ->
            tasks.forEach { task ->
                // Add retrieved thumbnails + remove unnecessary thumbnails
                task.thumbnail = thumbnailQueryResults[task.key.id]

                // TODO(b/352331675) don't load icons for DesktopTaskView
                // Add retrieved icons + remove unnecessary icons
                task.icon = iconQueryResults[task.key.id]?.icon
                task.titleDescription = iconQueryResults[task.key.id]?.contentDescription
                task.title = iconQueryResults[task.key.id]?.title
            }
            tasks
        }

    override fun getAllTaskData(forceRefresh: Boolean): Flow<List<Task>> {
        if (forceRefresh) {
            recentsModel.getTasks { groupedTaskData.value = it }
        }
        return taskData
    }

    override fun getTaskDataById(taskId: Int): Flow<Task?> =
        taskData.map { taskList -> taskList.firstOrNull { it.key.id == taskId } }

    override fun getThumbnailById(taskId: Int): Flow<ThumbnailData?> =
        getTaskDataById(taskId).map { it?.thumbnail }.distinctUntilChangedBy { it?.snapshotId }

    override fun setVisibleTasks(visibleTaskIdList: List<Int>) {
        this.visibleTaskIds.value = visibleTaskIdList.toSet()
    }

    /** Flow wrapper for [TaskThumbnailDataSource.getThumbnailInBackground] api */
    private fun getThumbnailDataRequest(task: Task): ThumbnailDataRequest =
        flow {
                emit(task.key.id to task.thumbnail)
                val thumbnailDataResult: ThumbnailData? =
                    suspendCancellableCoroutine { continuation ->
                        val cancellableTask =
                            taskThumbnailDataSource.getThumbnailInBackground(task) {
                                continuation.resume(it)
                            }
                        continuation.invokeOnCancellation { cancellableTask?.cancel() }
                    }
                emit(task.key.id to thumbnailDataResult)
            }
            .distinctUntilChanged()

    /**
     * This is a Flow that makes a query for thumbnail data to the [taskThumbnailDataSource] for
     * each visible task. It then collects the responses and returns them in a Map as soon as they
     * are available.
     */
    private fun getThumbnailQueryResults(): Flow<Map<Int, ThumbnailData?>> {
        val visibleTasks =
            combine(_taskData, visibleTaskIds) { tasks, visibleIds ->
                tasks.filter { it.key.id in visibleIds }
            }
        val visibleThumbnailDataRequests: Flow<List<ThumbnailDataRequest>> =
            visibleTasks.map { visibleTasksList -> visibleTasksList.map(::getThumbnailDataRequest) }
        return visibleThumbnailDataRequests.flatMapLatest {
            thumbnailRequestFlows: List<ThumbnailDataRequest> ->
            if (thumbnailRequestFlows.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(thumbnailRequestFlows) { it.toMap() }
            }
        }
    }

    /** Flow wrapper for [TaskThumbnailDataSource.getThumbnailInBackground] api */
    private fun getIconDataRequest(task: Task): IconDataRequest =
        flow {
                emit(task.key.id to task.getTaskIconQueryResponse())
                val iconDataResponse: TaskIconQueryResponse? =
                    suspendCancellableCoroutine { continuation ->
                        val cancellableTask =
                            taskIconDataSource.getIconInBackground(task) {
                                icon,
                                contentDescription,
                                title ->
                                continuation.resume(
                                    TaskIconQueryResponse(icon, contentDescription, title)
                                )
                            }
                        continuation.invokeOnCancellation { cancellableTask?.cancel() }
                    }
                emit(task.key.id to iconDataResponse)
            }
            .distinctUntilChanged()

    private fun getIconQueryResults(): Flow<Map<Int, TaskIconQueryResponse?>> {
        val visibleTasks =
            combine(_taskData, visibleTaskIds) { tasks, visibleIds ->
                tasks.filter { it.key.id in visibleIds }
            }
        val visibleIconDataRequests: Flow<List<IconDataRequest>> =
            visibleTasks.map { visibleTasksList -> visibleTasksList.map(::getIconDataRequest) }
        return visibleIconDataRequests.flatMapLatest { iconRequestFlows: List<IconDataRequest> ->
            if (iconRequestFlows.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(iconRequestFlows) { it.toMap() }
            }
        }
    }
}

private data class TaskIconQueryResponse(
    val icon: Drawable,
    val contentDescription: String,
    val title: String
)

private fun Task.getTaskIconQueryResponse(): TaskIconQueryResponse? {
    val iconVal = icon ?: return null
    val titleDescriptionVal = titleDescription ?: return null
    val titleVal = title ?: return null

    return TaskIconQueryResponse(iconVal, titleDescriptionVal, titleVal)
}

private typealias ThumbnailDataRequest = Flow<Pair<Int, ThumbnailData?>>

private typealias IconDataRequest = Flow<Pair<Int, TaskIconQueryResponse?>>
