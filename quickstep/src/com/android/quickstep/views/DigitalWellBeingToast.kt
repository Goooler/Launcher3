/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.AppUsageLimit
import android.graphics.Outline
import android.graphics.Paint
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.updateLayoutParams
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.util.Executors
import com.android.launcher3.util.SplitConfigurationOptions
import com.android.quickstep.TaskUtils
import com.android.systemui.shared.recents.model.Task
import java.time.Duration
import java.util.Locale

class DigitalWellBeingToast(
    private val container: RecentsViewContainer,
    private val taskView: TaskView
) {
    private val launcherApps: LauncherApps? =
        container.asContext().getSystemService(LauncherApps::class.java)

    private val bannerHeight =
        container
            .asContext()
            .resources
            .getDimensionPixelSize(R.dimen.digital_wellbeing_toast_height)

    private lateinit var task: Task

    private var appRemainingTimeMs: Long = 0
    private var banner: View? = null
    private var oldBannerOutlineProvider: ViewOutlineProvider? = null
    private var splitOffsetTranslationY = 0f
    private var splitOffsetTranslationX = 0f

    private var isDestroyed = false

    var hasLimit = false
    var splitBounds: SplitConfigurationOptions.SplitBounds? = null
    var bannerOffsetPercentage = 0f
        set(value) {
            if (field != value) {
                field = value
                banner?.let {
                    updateTranslationY()
                    it.invalidateOutline()
                }
            }
        }

    private fun setNoLimit() {
        hasLimit = false
        taskView.contentDescription = task.titleDescription
        replaceBanner(null)
        appRemainingTimeMs = -1
    }

    private fun setLimit(appUsageLimitTimeMs: Long, appRemainingTimeMs: Long) {
        this.appRemainingTimeMs = appRemainingTimeMs
        hasLimit = true
        val toast =
            container.viewCache
                .getView<TextView>(
                    R.layout.digital_wellbeing_toast,
                    container.asContext(),
                    taskView
                )
                .apply {
                    text =
                        Utilities.prefixTextWithIcon(
                            container.asContext(),
                            R.drawable.ic_hourglass_top,
                            getBannerText()
                        )
                    setOnClickListener(::openAppUsageSettings)
                }
        replaceBanner(toast)

        taskView.contentDescription =
            getContentDescriptionForTask(task, appUsageLimitTimeMs, appRemainingTimeMs)
    }

    fun initialize(task: Task) {
        check(!isDestroyed) { "Cannot re-initialize a destroyed toast" }
        this.task = task
        Executors.ORDERED_BG_EXECUTOR.execute {
            var usageLimit: AppUsageLimit? = null
            try {
                usageLimit =
                    launcherApps?.getAppUsageLimit(
                        this.task.topComponent.packageName,
                        UserHandle.of(this.task.key.userId)
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing digital well being toast", e)
            }
            val appUsageLimitTimeMs = usageLimit?.totalUsageLimit ?: -1
            val appRemainingTimeMs = usageLimit?.usageRemaining ?: -1
            taskView.post {
                if (isDestroyed) {
                    return@post
                }
                if (appUsageLimitTimeMs < 0 || appRemainingTimeMs < 0) {
                    setNoLimit()
                } else {
                    setLimit(appUsageLimitTimeMs, appRemainingTimeMs)
                }
            }
        }
    }

    /** Mark the DWB toast as destroyed and remove banner from TaskView. */
    fun destroy() {
        isDestroyed = true
        taskView.post { replaceBanner(null) }
    }

    private fun getSplitBannerConfig(): SplitBannerConfig {
        val splitBounds = splitBounds
        return when {
            splitBounds == null || !container.deviceProfile.isTablet || taskView.isLargeTile ->
                SplitBannerConfig.SPLIT_BANNER_FULLSCREEN
            // For portrait grid only height of task changes, not width. So we keep the text the
            // same
            !container.deviceProfile.isLeftRightSplit -> SplitBannerConfig.SPLIT_GRID_BANNER_LARGE
            // For landscape grid, for 30% width we only show icon, otherwise show icon and time
            task.key.id == splitBounds.leftTopTaskId ->
                if (splitBounds.leftTaskPercent < THRESHOLD_LEFT_ICON_ONLY)
                    SplitBannerConfig.SPLIT_GRID_BANNER_SMALL
                else SplitBannerConfig.SPLIT_GRID_BANNER_LARGE
            else ->
                if (splitBounds.leftTaskPercent > THRESHOLD_RIGHT_ICON_ONLY)
                    SplitBannerConfig.SPLIT_GRID_BANNER_SMALL
                else SplitBannerConfig.SPLIT_GRID_BANNER_LARGE
        }
    }

    private fun getReadableDuration(
        duration: Duration,
        @StringRes durationLessThanOneMinuteStringId: Int
    ): String {
        val hours = Math.toIntExact(duration.toHours())
        val minutes = Math.toIntExact(duration.minusHours(hours.toLong()).toMinutes())
        return when {
            // Apply FormatWidth.WIDE if both the hour part and the minute part are non-zero.
            hours > 0 && minutes > 0 ->
                MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.NARROW)
                    .formatMeasures(
                        Measure(hours, MeasureUnit.HOUR),
                        Measure(minutes, MeasureUnit.MINUTE)
                    )
            // Apply FormatWidth.WIDE if only the hour part is non-zero (unless forced).
            hours > 0 ->
                MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE)
                    .formatMeasures(Measure(hours, MeasureUnit.HOUR))
            // Apply FormatWidth.WIDE if only the minute part is non-zero (unless forced).
            minutes > 0 ->
                MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE)
                    .formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
            // Use a specific string for usage less than one minute but non-zero.
            duration > Duration.ZERO ->
                container.asContext().getString(durationLessThanOneMinuteStringId)
            // Otherwise, return 0-minute string.
            else ->
                MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE)
                    .formatMeasures(Measure(0, MeasureUnit.MINUTE))
        }
    }

    /**
     * Returns text to show for the banner depending on [.getSplitBannerConfig] If {@param
     * forContentDesc} is `true`, this will always return the full string corresponding to
     * [.SPLIT_BANNER_FULLSCREEN]
     */
    @JvmOverloads
    fun getBannerText(
        remainingTime: Long = appRemainingTimeMs,
        forContentDesc: Boolean = false
    ): String {
        val duration =
            Duration.ofMillis(
                if (remainingTime > MINUTE_MS)
                    (remainingTime + MINUTE_MS - 1) / MINUTE_MS * MINUTE_MS
                else remainingTime
            )
        val readableDuration =
            getReadableDuration(
                duration,
                R.string.shorter_duration_less_than_one_minute /* forceFormatWidth */
            )
        val splitBannerConfig = getSplitBannerConfig()
        return when {
            forContentDesc || splitBannerConfig == SplitBannerConfig.SPLIT_BANNER_FULLSCREEN ->
                container.asContext().getString(R.string.time_left_for_app, readableDuration)
            // show no text
            splitBannerConfig == SplitBannerConfig.SPLIT_GRID_BANNER_SMALL -> ""
            // SPLIT_GRID_BANNER_LARGE only show time
            else -> readableDuration
        }
    }

    private fun openAppUsageSettings(view: View) {
        val intent =
            Intent(OPEN_APP_USAGE_SETTINGS_TEMPLATE)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, task.topComponent.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        try {
            val options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.width, view.height)
            container.asContext().startActivity(intent, options.toBundle())

            // TODO: add WW logging on the app usage settings click.
        } catch (e: ActivityNotFoundException) {
            Log.e(
                TAG,
                "Failed to open app usage settings for task " + task.topComponent.packageName,
                e
            )
        }
    }

    private fun getContentDescriptionForTask(
        task: Task,
        appUsageLimitTimeMs: Long,
        appRemainingTimeMs: Long
    ): String =
        if (appUsageLimitTimeMs >= 0 && appRemainingTimeMs >= 0)
            container
                .asContext()
                .getString(
                    R.string.task_contents_description_with_remaining_time,
                    task.titleDescription,
                    getBannerText(appRemainingTimeMs, true /* forContentDesc */)
                )
        else task.titleDescription

    private fun replaceBanner(view: View?) {
        resetOldBanner()
        setBanner(view)
    }

    private fun resetOldBanner() {
        val banner = banner ?: return
        banner.outlineProvider = oldBannerOutlineProvider
        taskView.removeView(banner)
        banner.setOnClickListener(null)
        container.viewCache.recycleView(R.layout.digital_wellbeing_toast, banner)
    }

    private fun setBanner(banner: View?) {
        this.banner = banner
        if (banner != null && taskView.recentsView != null) {
            setupAndAddBanner()
            setBannerOutline()
        }
    }

    private fun setupAndAddBanner() {
        val banner = banner ?: return
        banner.updateLayoutParams<FrameLayout.LayoutParams> {
            bottomMargin =
                (taskView.firstSnapshotView.layoutParams as MarginLayoutParams).bottomMargin
        }
        val (translationX, translationY) =
            taskView.pagedOrientationHandler.getDwbLayoutTranslations(
                taskView.measuredWidth,
                taskView.measuredHeight,
                splitBounds,
                container.deviceProfile,
                taskView.snapshotViews,
                task.key.id,
                banner
            )
        splitOffsetTranslationX = translationX
        splitOffsetTranslationY = translationY
        updateTranslationY()
        updateTranslationX()
        taskView.addView(banner)
    }

    private fun setBannerOutline() {
        val banner = banner ?: return
        // TODO(b\273367585) to investigate why mBanner.getOutlineProvider() can be null
        val oldBannerOutlineProvider =
            if (banner.outlineProvider != null) banner.outlineProvider
            else ViewOutlineProvider.BACKGROUND
        this.oldBannerOutlineProvider = oldBannerOutlineProvider

        banner.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    oldBannerOutlineProvider.getOutline(view, outline)
                    val verticalTranslation = -view.translationY + splitOffsetTranslationY
                    outline.offset(0, Math.round(verticalTranslation))
                }
            }
        banner.clipToOutline = true
    }

    private fun updateTranslationY() {
        banner?.translationY = bannerOffsetPercentage * bannerHeight + splitOffsetTranslationY
    }

    private fun updateTranslationX() {
        banner?.translationX = splitOffsetTranslationX
    }

    fun setBannerColorTint(color: Int, amount: Float) {
        val banner = banner ?: return
        if (amount == 0f) {
            banner.setLayerType(View.LAYER_TYPE_NONE, null)
        }
        val layerPaint = Paint()
        layerPaint.setColorFilter(Utilities.makeColorTintingColorFilter(color, amount))
        banner.setLayerType(View.LAYER_TYPE_HARDWARE, layerPaint)
        banner.setLayerPaint(layerPaint)
    }

    fun setBannerVisibility(visibility: Int) {
        banner?.visibility = visibility
    }

    private fun getAccessibilityActionId(): Int =
        if (splitBounds?.rightBottomTaskId == task.key.id)
            R.id.action_digital_wellbeing_bottom_right
        else R.id.action_digital_wellbeing_top_left

    fun getDWBAccessibilityAction(): AccessibilityNodeInfo.AccessibilityAction? {
        if (!hasLimit) return null
        val context = container.asContext()
        val label =
            if ((taskView.containsMultipleTasks()))
                context.getString(
                    R.string.split_app_usage_settings,
                    TaskUtils.getTitle(context, task)
                )
            else context.getString(R.string.accessibility_app_usage_settings)
        return AccessibilityNodeInfo.AccessibilityAction(getAccessibilityActionId(), label)
    }

    fun handleAccessibilityAction(action: Int): Boolean {
        if (getAccessibilityActionId() != action) return false
        openAppUsageSettings(taskView)
        return true
    }

    companion object {
        private const val THRESHOLD_LEFT_ICON_ONLY = 0.4f
        private const val THRESHOLD_RIGHT_ICON_ONLY = 0.6f

        enum class SplitBannerConfig {
            /** Will span entire width of taskView with full text */
            SPLIT_BANNER_FULLSCREEN,
            /** Used for grid task view, only showing icon and time */
            SPLIT_GRID_BANNER_LARGE,
            /** Used for grid task view, only showing icon */
            SPLIT_GRID_BANNER_SMALL
        }

        val OPEN_APP_USAGE_SETTINGS_TEMPLATE: Intent = Intent(Settings.ACTION_APP_USAGE_SETTINGS)
        const val MINUTE_MS: Int = 60000

        private const val TAG = "DigitalWellBeingToast"
    }
}
