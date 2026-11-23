package com.zerotwodev.recall.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons from the design canvas, drawn from their SVG paths (24×24, round strokes).
 * Defined here instead of a Material Icons dependency so they look the same on every platform.
 */
object RecallIcons {
    val Mic = icon("Mic", "M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z", "M5 11a7 7 0 0 0 14 0M12 18v3")
    val Pen = icon("Pen", "M4 20h4L19 9l-4-4L4 16v4z", "M13.5 6.5l4 4")
    val Camera = icon("Camera", "M4 8h3l2-3h6l2 3h3v11H4z", "M8.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0")
    val Lock = icon(
        "Lock",
        "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
        "M8 11V8a4 4 0 0 1 8 0v3",
    )
    val Home = icon("Home", "M4 11l8-7 8 7v9h-5v-6H9v6H4z")
    val Timeline = icon(
        "Timeline",
        "M9 6h11M9 12h11M9 18h11",
        "M3.3 6a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0",
        "M3.3 12a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0",
        "M3.3 18a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0",
    )
    val Settings = icon(
        "Settings",
        "M4 7h10M18 7h2M4 17h4M12 17h8",
        "M14 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        "M8 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
    )
    val Search = icon("Search", "M5 11a6 6 0 1 0 12 0a6 6 0 1 0-12 0", "M20 20l-4.5-4.5")
    val Back = icon("Back", "M19 12H5", "M11 6l-6 6 6 6")
    val More = icon("More", "M12 5.5v.01M12 12v.01M12 18.5v.01", strokeWidth = 2.6f)

    private fun icon(name: String, vararg paths: String, strokeWidth: Float = 1.8f): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black), // Icon() tints this with its color
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
