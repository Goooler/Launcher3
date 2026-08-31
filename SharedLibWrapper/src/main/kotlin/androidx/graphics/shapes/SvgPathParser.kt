package androidx.graphics.shapes

import android.graphics.Path
import androidx.core.graphics.PathParser

class Feature

object SvgPathParser {
    @JvmStatic
    fun parseFeatures(pathData: String): List<Feature> = emptyList()

    @JvmStatic
    fun parsePath(pathData: String): Path {
        return PathParser.createPathFromPathData(pathData)
    }
}

@Suppress("FunctionName")
fun RoundedPolygon(features: List<Feature>, centerX: Float = 0f, centerY: Float = 0f): RoundedPolygon {
    return RoundedPolygon(4, 1f, centerX, centerY)
}
