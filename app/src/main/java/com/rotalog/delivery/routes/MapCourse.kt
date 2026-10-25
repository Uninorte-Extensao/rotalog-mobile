package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.Route

/** Mapa do percurso, para telas que não têm bottom sheet. */
@Composable
fun MapCourse(
    routeMap: RouteMap,
    route: Route,
    modifier: Modifier = Modifier,
    altura: Dp = 240.dp,
) {
    Box(modifier.fillMaxWidth().height(altura).clip(MaterialTheme.shapes.large)) {
        routeMap.Content(
            stops = route.stops,
            selectedStopId = null,
            nextStopId = route.nextStop?.id,
            topInset = 0.dp,
            bottomInset = 0.dp,
            onSelectStop = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}