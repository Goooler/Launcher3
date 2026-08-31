package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

@RequiresOptIn(
    "This is an experimental Material3 Expressive API.",
    RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalMaterial3ExpressiveApi

public val Typography.headlineSmallEmphasized: TextStyle
    get() = headlineSmall

public val Typography.titleMediumEmphasized: TextStyle
    get() = titleMedium

public val Typography.labelLargeEmphasized: TextStyle
    get() = labelLarge

public interface MotionScheme {
    fun <T> slowSpatialSpec(): AnimationSpec<T>
}

public val MaterialTheme.motionScheme: MotionScheme
    @Composable
    @ReadOnlyComposable
    get() = object : MotionScheme {
        override fun <T> slowSpatialSpec(): AnimationSpec<T> = spring()
    }
