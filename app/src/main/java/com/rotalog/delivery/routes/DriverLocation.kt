package com.rotalog.delivery.routes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.maplibre.spatialk.geojson.Position

private const val FINA = Manifest.permission.ACCESS_FINE_LOCATION
private const val APROXIMADA = Manifest.permission.ACCESS_COARSE_LOCATION

private fun Context.temPermissao(permissao: String): Boolean =
    ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED

private fun Location.paraPosition(): Position =
    Position(longitude = longitude, latitude = latitude)

/** Posição do entregador em tempo real, ou null enquanto não há permissão ou sinal. */
@SuppressLint("MissingPermission") // as permissões são checadas antes de cada uso
@Composable
fun rememberPosicaoEntregador(): Position? {
    val context = LocalContext.current

    var fina by remember { mutableStateOf(context.temPermissao(FINA)) }
    var aproximada by remember { mutableStateOf(context.temPermissao(APROXIMADA)) }
    var jaPediu by rememberSaveable { mutableStateOf(false) }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        fina = context.temPermissao(FINA)
        aproximada = context.temPermissao(APROXIMADA)
    }

    LaunchedEffect(Unit) {
        if (!fina && !aproximada && !jaPediu) {
            jaPediu = true // não insiste se o entregador negar
            pedirPermissao.launch(arrayOf(FINA, APROXIMADA))
        }
    }

    var posicao by remember { mutableStateOf<Position?>(null) }

    DisposableEffect(fina, aproximada) {
        if (!fina && !aproximada) {
            return@DisposableEffect onDispose { }
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { loc -> posicao = loc.paraPosition() }

        // GPS exige permissão precisa; com a aproximada, usa a rede (Wi-Fi/celular)
        val provedor = if (fina && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

        try {
            // mostra algo já, enquanto o GPS ainda não deu o primeiro sinal
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
                ?.let { posicao = it.paraPosition() }

            lm.requestLocationUpdates(
                provedor,
                2000L, // no máximo a cada 2 s
                5f,    // ou a cada 5 m percorridos
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            // permissão revogada durante o uso
        } catch (_: IllegalArgumentException) {
            // aparelho sem esse provedor
        }

        onDispose { lm.removeUpdates(listener) }
    }

    return posicao
}