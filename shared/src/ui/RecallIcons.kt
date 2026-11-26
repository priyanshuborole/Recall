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
    /** Open book for the empty Timeline (board 6b), drawn on a 148×104 canvas. */
    val Book = ImageVector.Builder("Book", 148.dp, 104.dp, 148f, 104f).apply {
        listOf(
            "M74 22c-14-9-36-11-58-6v74c22-5 44-3 58 6 14-9 36-11 58-6V16c-22-5-44-3-58 6z",
            "M74 22v74",
            "M28 36c12-2 24-1 34 3M28 48c12-2 24-1 34 3M28 60c8-1 16-1 22 1",
            "M86 39c10-4 22-5 34-3M86 51c6-2 12-3 18-3",
            "M102 68a10 10 0 1 0 20 0a10 10 0 1 0-20 0",
            "M112 63v6M108.5 67.5a3.5 3.5 0 0 0 7 0M112 71v2",
        ).forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
    val Fingerprint = icon(
        "Fingerprint",
        "M12 11.5v4.5", "M8.5 12a3.5 3.5 0 0 1 7 0v3.5", "M6 12a6 6 0 0 1 12 0v2", "M6 15.5a10 10 0 0 0 1.5 4",
        "M3.5 12a8.5 8.5 0 0 1 3-6.5", "M9 4a8.5 8.5 0 0 1 11.5 8v1", "M12 19.5c-.8-1-1.3-2-1.5-3",
        strokeWidth = 1.5f,
    )
    val Shield = icon("Shield", "M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z", strokeWidth = 2f)
    val Close = icon("Close", "M6 6l12 12M18 6L6 18")
    val Check = icon("Check", "M5 12l5 5 9-10", strokeWidth = 2.2f)
    val Plus = icon("Plus", "M12 5v14M5 12h14")
    val Pin = icon("Pin", "M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12z", "M9.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0")
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
