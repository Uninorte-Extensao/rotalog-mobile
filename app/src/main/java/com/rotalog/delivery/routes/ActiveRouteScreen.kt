package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import androidx.compose.ui.platform.testTag
import com.rotalog.delivery.shared.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties

@Composable
internal fun ActiveRoute(
    state: RoutesUiState,
    onSelectStop: (String) -> Unit,
    onDetailsChange: (Boolean) -> Unit,
    onShowStops: (String) -> Unit,
    onNextStop: () -> Unit,
    routeMap: RouteMap
) {
    val route = state.route ?: return
    val selected = state.selectedStop ?: return
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }
    var summaryHeight by remember { mutableIntStateOf(0) }
    val detailsFocus = remember { FocusRequester() }
    var wasOpen by remember { mutableStateOf(false) }
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(state.detailsOpen, windowFocused) {
        if (state.detailsOpen) wasOpen = true
        else if (wasOpen && windowFocused) {
            // The Dialog must release the window before its trigger can regain focus.
            withFrameNanos { }
            val focused = detailsFocus.requestFocus()
            if (focused) wasOpen = false
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        routeMap.Content(route.stops, selected.id, route.nextStop?.id,
            with(density) { headerHeight.toDp() }, with(density) { summaryHeight.toDp() }, onSelectStop,
            Modifier.fillMaxSize().testTag("route-map"))
        Surface(Modifier.align(Alignment.TopCenter).padding(12.dp).onSizeChanged { headerHeight = it.height + with(density) { 24.dp.roundToPx() } },
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(route.code, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${route.completedStops}/${route.stops.size}", style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.semantics { contentDescription = "${route.completedStops} de ${route.stops.size} concluídas" })
                    IconButton(onClick = { onShowStops(route.id) }, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcons.List, "Lista de paradas")
                    }
                }
                LinearProgressIndicator(progress = { route.progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        SectionCard(Modifier.align(Alignment.BottomCenter).padding(12.dp).heightIn(max = maxHeight * .4f)
            .onSizeChanged { summaryHeight = it.height + with(density) { 24.dp.roundToPx() } }) {
            // Keep the action outside the scrolling brief, including with 200% font.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (selected.id == route.nextStop?.id) "PRÓXIMA PARADA" else "PARADA SELECIONADA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("${selected.sequence}. ${selected.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { contentDescription = selected.name })
                Text(selected.address, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = selected.address }, style = MaterialTheme.typography.bodySmall)
                Text("${selected.type.label} • ${selected.status.label}", style = MaterialTheme.typography.labelMedium)
                if (selected.id != route.nextStop?.id && route.nextStop != null) {
                    TextButton(onClick = onNextStop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Voltar à próxima parada") }
                }
            }
            PrimaryAction("Ver detalhes", { onDetailsChange(true) }, Modifier.focusRequester(detailsFocus).focusProperties { canFocus = true })
        }
    }
}
