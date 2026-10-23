package com.rotalog.delivery.routes

import org.maplibre.spatialk.geojson.Position

internal const val GEOJSON_VAZIO = """{"type":"FeatureCollection","features":[]}"""

internal fun pontosGeoJson(pontos: List<Position>): String {
    if (pontos.isEmpty()) return GEOJSON_VAZIO
    val features = pontos.joinToString(",") {
        """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${it.longitude},${it.latitude}]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

/** Pontos com o número da parada na propriedade "numero" (texto), para o rótulo no mapa. */
internal fun numerosGeoJson(paradas: List<Pair<Position, Int>>): String {
    if (paradas.isEmpty()) return GEOJSON_VAZIO
    val features = paradas.joinToString(",") { (p, numero) ->
        """{"type":"Feature","properties":{"numero":"$numero"},"geometry":{"type":"Point","coordinates":[${p.longitude},${p.latitude}]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

internal fun linhaGeoJson(pontos: List<Position>): String {
    if (pontos.size < 2) return GEOJSON_VAZIO
    val coords = pontos.joinToString(",") { "[${it.longitude},${it.latitude}]" }
    return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coords]}}"""
}