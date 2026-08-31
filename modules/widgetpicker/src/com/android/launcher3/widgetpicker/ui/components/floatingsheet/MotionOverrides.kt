package com.android.launcher3.widgetpicker.ui.components.floatingsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

interface MotionScheme {
    fun <T> slowSpatialSpec(): AnimationSpec<T>
}

val MaterialTheme.motionScheme: MotionScheme
    @Composable
    @ReadOnlyComposable
    get() = object : MotionScheme {
        override fun <T> slowSpatialSpec(): AnimationSpec<T> = spring()
    }
