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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.util.ViewPool
import com.android.quickstep.task.thumbnail.TaskThumbnailUiState.BackgroundOnly
import com.android.quickstep.task.thumbnail.TaskThumbnailUiState.LiveTile
import com.android.quickstep.task.thumbnail.TaskThumbnailUiState.Snapshot
import com.android.quickstep.task.thumbnail.TaskThumbnailUiState.Uninitialized
import com.android.quickstep.util.TaskCornerRadius
import com.android.quickstep.views.RecentsView
import com.android.quickstep.views.RecentsViewContainer
import com.android.quickstep.views.TaskView
import com.android.systemui.shared.system.QuickStepContract
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TaskThumbnailView : FrameLayout, ViewPool.Reusable {
    // TODO(b/335649589): Ideally create and obtain this from DI. This ViewModel should be scoped
    //  to [TaskView], and also shared between [TaskView] and [TaskThumbnailView]
    //  This is using a lazy for now because the dependencies cannot be obtained without DI.
    val viewModel by lazy {
        val recentsView =
            RecentsViewContainer.containerFromContext<RecentsViewContainer>(context)
                .getOverviewPanel<RecentsView<*, *>>()
        TaskThumbnailViewModel(
            recentsView.mRecentsViewData!!,
            (parent as TaskView).taskViewData,
            (parent as TaskView).getTaskContainerForTaskThumbnailView(this)!!.taskContainerData,
            recentsView.mTasksRepository!!,
        )
    }

    private val scrimView: View by lazy { findViewById(R.id.task_thumbnail_scrim) }
    private val liveTileView: LiveTileView by lazy { findViewById(R.id.task_thumbnail_live_tile) }
    private val thumbnail: ImageView by lazy { findViewById(R.id.task_thumbnail) }

    private var inheritedScale: Float = 1f

    private val _measuredBounds = Rect()
    private val measuredBounds: Rect
        get() {
            _measuredBounds.set(0, 0, measuredWidth, measuredHeight)
            return _measuredBounds
        }

    private var overviewCornerRadius: Float = TaskCornerRadius.get(context)
    private var fullscreenCornerRadius: Float = QuickStepContract.getWindowCornerRadius(context)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // TODO(b/335396935) replace MainScope with shorter lifecycle.
        MainScope().launch {
            viewModel.uiState.collect { viewModelUiState ->
                resetViews()
                when (viewModelUiState) {
                    is Uninitialized -> {}
                    is LiveTile -> drawLiveWindow()
                    is Snapshot -> drawSnapshot(viewModelUiState)
                    is BackgroundOnly -> drawBackground(viewModelUiState.backgroundColor)
                }
            }
        }
        MainScope().launch {
            viewModel.dimProgress.collect { dimProgress ->
                // TODO(b/348195366) Add fade in/out for scrim
                scrimView.alpha = dimProgress * MAX_SCRIM_ALPHA
            }
        }
        MainScope().launch { viewModel.cornerRadiusProgress.collect { invalidateOutline() } }
        MainScope().launch {
            viewModel.inheritedScale.collect { viewModelInheritedScale ->
                inheritedScale = viewModelInheritedScale
                invalidateOutline()
            }
        }

        clipToOutline = true
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(measuredBounds, getCurrentCornerRadius())
                }
            }
    }

    override fun onRecycle() {
        // Do nothing
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        overviewCornerRadius = TaskCornerRadius.get(context)
        fullscreenCornerRadius = QuickStepContract.getWindowCornerRadius(context)
        invalidateOutline()
    }

    private fun resetViews() {
        liveTileView.isVisible = false
        thumbnail.isVisible = false
        scrimView.alpha = 0f
        setBackgroundColor(Color.BLACK)
    }

    private fun drawBackground(@ColorInt background: Int) {
        setBackgroundColor(background)
    }

    private fun drawLiveWindow() {
        liveTileView.isVisible = true
    }

    private fun drawSnapshot(snapshot: Snapshot) {
        drawBackground(snapshot.backgroundColor)
        thumbnail.setImageBitmap(snapshot.bitmap)
        thumbnail.isVisible = true
    }

    private fun getCurrentCornerRadius() =
        Utilities.mapRange(
            viewModel.cornerRadiusProgress.value,
            overviewCornerRadius,
            fullscreenCornerRadius
        ) / inheritedScale

    private companion object {
        const val MAX_SCRIM_ALPHA = 0.4f
    }
}
