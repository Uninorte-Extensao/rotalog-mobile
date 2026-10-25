package com.rotalog.delivery.routes

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.rotalog.delivery.model.Stop
import com.rotalog.delivery.model.StopStatus
import com.rotalog.delivery.model.StopType
import kotlinx.coroutines.CancellationException
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

object MapLibreRouteMap : RouteMap {
    @Composable
    override fun Content(
        stops: List<Stop>, selectedStopId: String?, nextStopId: String?,
        topInset: Dp, bottomInset: Dp, onSelectStop: (String) -> Unit,
        modifier: Modifier
    ) {
        MapaMapLibre(stops, selectedStopId, nextStopId, topInset, bottomInset, modifier)
    }
}

private fun Stop.pos() = Position(longitude = longitude, latitude = latitude)

@Composable
private fun MapaMapLibre(
    stops: List<Stop>,
    selectedStopId: String?,
    nextStopId: String?,
    topInset: Dp,
    bottomInset: Dp,
    modifier: Modifier,
) {
    val ordenadas = remember(stops) { stops.sortedBy { it.sequence } }
    val linha = ordenadas.map { it.pos() }
    // o CD (coleta) é sempre o ponto azul e é a parada 1; as entregas ficam cinza/verde/laranja
    val cd = ordenadas.filter { it.type == StopType.COLLECTION }.map { it.pos() }
    val entregas = ordenadas.filter { it.type != StopType.COLLECTION }
    val concluidas = entregas.filter { it.status == StopStatus.COMPLETED }.map { it.pos() }
    val proxima = entregas.filter { it.id == nextStopId }.map { it.pos() }
    val pendentes = entregas.filter {
        it.status != StopStatus.COMPLETED && it.id != nextStopId
    }.map { it.pos() }
    val selecionada = ordenadas.filter { it.id == selectedStopId }.map { it.pos()}
    // número de cada parada no mapa = a ordem (sequence) da parada na rota (o CD é a 1)
    val numeradas = ordenadas.map { it.pos() to it.sequence }

    val posicao = rememberPosicaoEntregador()

    // Rota pelas ruas (OSRM) passando por todas as paradas, na ordem.
    // Enquanto não chega (ou se falhar / sem internet), a linha reta entre as paradas serve de reserva.
    var rota by remember { mutableStateOf<RotaRuas?>(null) }
    val chaveRota = linha.joinToString(";") { "${it.longitude},${it.latitude}" }
    LaunchedEffect(chaveRota) {
        if (linha.size < 2) {
            rota = null
            return@LaunchedEffect
        }
        try {
            rota = buscarRota(linha)
        } catch (e: CancellationException) {
            throw e // cancelamento não é falha de rota
        } catch (e: Exception) {
            Log.e("Rota", "Falha ao buscar rota", e)
            rota = null
        }
    }
    val pontosDaLinha = rota?.pontos ?: linha

    // as bordas mudam quando a sheet é arrastada; os efeitos leem o valor mais recente
    val topoAtual by rememberUpdatedState(topInset + 24.dp)
    val baseAtual by rememberUpdatedState(bottomInset + 24.dp)

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        initialCameraPosition = CameraPosition(
            center = linha.firstOrNull() ?: Position(longitude = -60.0217, latitude = -3.1190),
            zoom = 11.0,
        ),
    ) {
        val fonteRota = rememberGeoJsonSource(GeoJsonData.JsonString(linhaGeoJson(pontosDaLinha)))
        val fonteConcluidas = rememberGeoJsonSource(GeoJsonData.JsonString(pontosGeoJson(concluidas)))
        val fontePendentes = rememberGeoJsonSource(GeoJsonData.JsonString(pontosGeoJson(pendentes)))
        val fonteProxima = rememberGeoJsonSource(GeoJsonData.JsonString(pontosGeoJson(proxima)))
        val fonteSelecionada = rememberGeoJsonSource(GeoJsonData.JsonString(pontosGeoJson(selecionada)))
        val fonteNumeros = rememberGeoJsonSource(GeoJsonData.JsonString(numerosGeoJson(numeradas)))
        val fonteCd = rememberGeoJsonSource(GeoJsonData.JsonString(pontosGeoJson(cd)))
        val fonteEntregador = rememberGeoJsonSource(
            GeoJsonData.JsonString(pontosGeoJson(listOfNotNull(posicao)))
        )

        LineLayer(id = "rota", source = fonteRota,
            color = const(Color(0xFF1971C2)), width = const(5.dp))

        // anel de destaque da parada selecionada (fica por baixo dos círculos)
        CircleLayer(id = "selecionada", source = fonteSelecionada,
            color = const(Color(0xFFF59F00)), radius = const(17.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))

        CircleLayer(id = "pendentes", source = fontePendentes,
            color = const(Color(0xFF495057)), radius = const(9.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))
        CircleLayer(id = "concluidas", source = fonteConcluidas,
            color = const(Color(0xFF2F9E44)), radius = const(9.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))
        // próxima entrega (laranja: o azul é só do CD)
        CircleLayer(id = "proxima", source = fonteProxima,
            color = const(Color(0xFFE8590C)), radius = const(12.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))
        // CD: ponto azul, parada 1
        CircleLayer(id = "cd", source = fonteCd,
            color = const(Color(0xFF2563EB)), radius = const(12.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))

        // número de cada parada, ao lado (acima e à direita) do ponto
        SymbolLayer(
            id = "numeros", source = fonteNumeros,
            textField = format(span(feature["numero"].asString())),
            textColor = const(Color(0xFF212529)),
            textHaloColor = const(Color.White), textHaloWidth = const(2.dp),
            textFont = const(listOf("Noto Sans Bold")), // fonte que o estilo Liberty do OpenFreeMap oferece
            textSize = const(1.em),
            textAnchor = const(SymbolAnchor.BottomLeft),
            textOffset = textOffset(0.9f.em, (-0.3f).em),
            textAllowOverlap = const(true), textIgnorePlacement = const(true),
        )

        // halo translúcido + ponto do entregador
        CircleLayer(id = "entregador-halo", source = fonteEntregador,
            color = const(Color(0x337048E8)), radius = const(20.dp))
        CircleLayer(id = "entregador", source = fonteEntregador,
            color = const(Color(0xFF7048E8)), radius = const(8.dp),
            strokeColor = const(Color.White), strokeWidth = const(3.dp))
    }

    // enquadra a rota inteira na primeira vez
    LaunchedEffect(linha.size) {
        if (linha.size < 2) return@LaunchedEffect
        mapState.animateCameraToBounds(
            boundingBox = BoundingBox(
                west = linha.minOf { it.longitude }, south = linha.minOf { it.latitude },
                east = linha.maxOf { it.longitude }, north = linha.maxOf { it.latitude },
            ),
            fit = CameraFit(
                fitPadding = DpPadding(left = 48.dp, top = topoAtual, right = 48.dp, bottom = baseAtual)
            ),
        )
    }

    // ao selecionar outra parada, vai até ela (caixa pequena em volta = zoom ~15)
    var primeiraSelecao by remember { mutableStateOf(true) }
    LaunchedEffect(selectedStopId) {
        if (primeiraSelecao) { primeiraSelecao = false; return@LaunchedEffect } // mantém a visão geral
        val p = ordenadas.firstOrNull { it.id == selectedStopId }?.pos() ?: return@LaunchedEffect
        val d = 0.004
        mapState.animateCameraToBounds(
            boundingBox = BoundingBox(
                west = p.longitude - d, south = p.latitude - d,
                east = p.longitude + d, north = p.latitude + d,
            ),
            fit = CameraFit(
                fitPadding = DpPadding(left = 48.dp, top = topoAtual, right = 48.dp, bottom = baseAtual)
            ),
        )
    }

    Box(modifier) {
        MaplibreMap(modifier = Modifier.fillMaxSize(), state = mapState)
    }
}