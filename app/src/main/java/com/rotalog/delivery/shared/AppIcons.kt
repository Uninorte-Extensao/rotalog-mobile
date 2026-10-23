package com.rotalog.delivery.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Native vectors matching the reference without an icon font dependency. */
object AppIcons {
    private fun icon(name: String, draw: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, pathBuilder = draw)
        }.build()
    val Truck = icon("Caminhão") {
        moveTo(2f, 5f); lineTo(15f, 5f); lineTo(15f, 17f); lineTo(2f, 17f); close()
        moveTo(15f, 9f); lineTo(19f, 9f); lineTo(22f, 13f); lineTo(22f, 17f); lineTo(15f, 17f)
        moveTo(8f, 18f); arcTo(2f, 2f, 0f, true, true, 4f, 18f); arcTo(2f, 2f, 0f, true, true, 8f, 18f)
        moveTo(20f, 18f); arcTo(2f, 2f, 0f, true, true, 16f, 18f); arcTo(2f, 2f, 0f, true, true, 20f, 18f)
    }
    val List = icon("Paradas") { moveTo(3f,6f); lineTo(5f,6f); moveTo(8f,6f); lineTo(21f,6f); moveTo(3f,12f); lineTo(5f,12f); moveTo(8f,12f); lineTo(21f,12f); moveTo(3f,18f); lineTo(5f,18f); moveTo(8f,18f); lineTo(21f,18f) }
    val Back = icon("Voltar") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    val Check = icon("Concluído") { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) }
    val Pin = icon("Endereço") { moveTo(12f, 22f); curveTo(-7f, 4f, 31f, -6f, 12f, 22f); close(); moveTo(15f, 9f); arcTo(3f,3f,0f,true,true,9f,9f); arcTo(3f,3f,0f,true,true,15f,9f) }
    val Cargo = icon("Carga") { moveTo(3f, 6f); lineTo(12f, 2f); lineTo(21f, 6f); lineTo(21f, 19f); lineTo(12f, 23f); lineTo(3f,19f); close(); moveTo(3f,6f); lineTo(12f,10f); lineTo(21f,6f); moveTo(12f,10f); lineTo(12f,23f) }
    val Phone = icon("Telefone") { moveTo(4f,3f); lineTo(8f,3f); lineTo(10f,8f); lineTo(7f,10f); curveTo(8f,14f,10f,16f,14f,17f); lineTo(16f,14f); lineTo(21f,16f); lineTo(21f,20f); curveTo(13f,26f,-2f,10f,4f,3f); close() }
    val Calendar = icon("Data") { moveTo(3f,5f); lineTo(21f,5f); lineTo(21f,22f); lineTo(3f,22f); close(); moveTo(3f,10f); lineTo(21f,10f); moveTo(8f,2f); lineTo(8f,7f); moveTo(16f,2f); lineTo(16f,7f) }
    val Alert = icon("Atenção") { moveTo(12f,2f); lineTo(23f,21f); lineTo(1f,21f); close(); moveTo(12f,8f); lineTo(12f,14f); moveTo(12f,17f); lineTo(12f,18f) }
}
