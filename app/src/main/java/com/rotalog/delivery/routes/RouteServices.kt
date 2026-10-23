package com.rotalog.delivery.routes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.spatialk.geojson.Position
import java.net.HttpURLConnection
import java.net.URL

// Servidor demo do OSRM: uso leve; para produção, use um serviço próprio
private const val OSRM_URL = "https://router.project-osrm.org/route/v1/driving"

data class RotaRuas(
    val pontos: List<Position>,
    val distanciaM: Double,
    val duracaoS: Double,
)

/** Rota pelas ruas passando por todas as paradas, na ordem recebida. */
suspend fun buscarRota(paradas: List<Position>): RotaRuas = withContext(Dispatchers.IO) {
    require(paradas.size >= 2) { "Precisa de pelo menos 2 pontos" }
    val coordenadas = paradas.joinToString(";") { "${it.longitude},${it.latitude}" }
    val url = URL("$OSRM_URL/$coordenadas?overview=full&geometries=geojson")
    val conexao = url.openConnection() as HttpURLConnection
    try {
        conexao.connectTimeout = 10_000
        conexao.readTimeout = 10_000
        conexao.setRequestProperty("User-Agent", "RotaLog/1.0")
        if (conexao.responseCode != 200) error("OSRM HTTP ${conexao.responseCode}")

        val texto = conexao.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(texto)
        if (json.getString("code") != "Ok") error("Rota não encontrada")

        val rota = json.getJSONArray("routes").getJSONObject(0)
        val coords = rota.getJSONObject("geometry").getJSONArray("coordinates")
        val pontos = List(coords.length()) { i ->
            val c = coords.getJSONArray(i)
            Position(longitude = c.getDouble(0), latitude = c.getDouble(1))
        }
        RotaRuas(pontos, rota.getDouble("distance"), rota.getDouble("duration"))
    } finally {
        conexao.disconnect()
    }
}