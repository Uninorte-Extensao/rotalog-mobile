package com.rotalog.delivery.login

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Native vectors matching the reference without an icon font dependency. */
internal object LoginIcons {
    private fun icon(name: String, draw: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, pathBuilder = draw)
        }.build()
    val Mail = icon("E-mail") {
        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(3f, 19f); close()
        moveTo(3f, 6f); lineTo(12f, 13f); lineTo(21f, 6f)
    }
    val Lock = icon("Senha") {
        moveTo(5f, 10f); lineTo(19f, 10f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(8f, 10f); lineTo(8f, 7f); curveTo(8f, 1f, 16f, 1f, 16f, 7f); lineTo(16f, 10f)
        moveTo(12f, 14f); lineTo(12f, 17f)
    }
    val Eye = icon("Visibilidade") {
        moveTo(2f, 12f); curveTo(7f, 4f, 17f, 4f, 22f, 12f); curveTo(17f, 20f, 7f, 20f, 2f, 12f); close()
        moveTo(15f, 12f); arcTo(3f, 3f, 0f, true, true, 9f, 12f); arcTo(3f, 3f, 0f, true, true, 15f, 12f)
    }
    val EyeOff = icon("Ocultar") {
        moveTo(3f, 3f); lineTo(21f, 21f)
        moveTo(2f, 12f); curveTo(4f, 9f, 5f, 8f, 7f, 7f)
        moveTo(10f, 6f); curveTo(15f, 5f, 19f, 8f, 22f, 12f); lineTo(19f, 16f)
        moveTo(16f, 18f); curveTo(10f, 20f, 5f, 17f, 2f, 12f)
    }
}
