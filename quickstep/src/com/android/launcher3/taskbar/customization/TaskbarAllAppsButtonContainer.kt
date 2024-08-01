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

package com.android.launcher3.taskbar.customization

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import com.android.launcher3.R
import com.android.launcher3.Utilities.dpToPx
import com.android.launcher3.config.FeatureFlags
import com.android.launcher3.taskbar.TaskbarActivityContext
import com.android.launcher3.views.ActivityContext
import com.android.launcher3.views.IconButtonView

/** Taskbar all apps button container for customizable taskbar. */
class TaskbarAllAppsButtonContainer
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs), TaskbarContainer {

    private val allAppsButton: IconButtonView =
        LayoutInflater.from(context).inflate(R.layout.taskbar_all_apps_button, this, false)
            as IconButtonView
    private val activityContext: TaskbarActivityContext = ActivityContext.lookupContext(context)

    override val spaceNeeded: Int
        get() {
            return dpToPx(activityContext.taskbarSpecsEvaluator!!.taskbarIconSize.size.toFloat())
        }

    init {
        setUpIcon()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ResourceAsColor")
    private fun setUpIcon() {
        val drawable =
            resources.getDrawable(
                getAllAppsButton(activityContext.taskbarFeatureEvaluator!!.isTransient)
            )
        val padding = activityContext.taskbarSpecsEvaluator!!.taskbarIconPadding

        allAppsButton.setIconDrawable(drawable)
        allAppsButton.setPadding(padding)
        allAppsButton.setForegroundTint(activityContext.getColor(R.color.all_apps_button_color))

        // TODO(b/356465292) : add click listeners in future cl
        addView(allAppsButton)
    }

    @DrawableRes
    private fun getAllAppsButton(isTransientTaskbar: Boolean): Int {
        val shouldSelectTransientIcon =
            isTransientTaskbar ||
                (FeatureFlags.enableTaskbarPinning() && !activityContext.isThreeButtonNav)
        return if (FeatureFlags.ENABLE_ALL_APPS_SEARCH_IN_TASKBAR.get()) {
            if (shouldSelectTransientIcon) R.drawable.ic_transient_taskbar_all_apps_search_button
            else R.drawable.ic_taskbar_all_apps_search_button
        } else {
            if (shouldSelectTransientIcon) R.drawable.ic_transient_taskbar_all_apps_button
            else R.drawable.ic_taskbar_all_apps_button
        }
    }

    @DimenRes
    fun getAllAppsButtonTranslationXOffset(isTransientTaskbar: Boolean): Int {
        return if (isTransientTaskbar) {
            R.dimen.transient_taskbar_all_apps_button_translation_x_offset
        } else if (FeatureFlags.ENABLE_ALL_APPS_SEARCH_IN_TASKBAR.get()) {
            R.dimen.taskbar_all_apps_search_button_translation_x_offset
        } else {
            R.dimen.taskbar_all_apps_button_translation_x_offset
        }
    }
}
