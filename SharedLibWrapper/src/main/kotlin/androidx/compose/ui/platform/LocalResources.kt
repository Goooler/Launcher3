package androidx.compose.ui.platform

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object LocalResources {
    val current: Resources
        @Composable
        @ReadOnlyComposable
        get() = LocalContext.current.resources
}
