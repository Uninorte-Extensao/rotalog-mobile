package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.*

@Composable
fun RoutesScreen(
    state: RoutesUiState,
    onAccept: () -> Unit,
    onSelectStop: (String) -> Unit,
    onDetailsChange: (Boolean) -> Unit,
    onShowStops: (String) -> Unit,
    onNavigate: (Stop) -> Unit,
    onCall: (Stop) -> String?,
    onNextStop: () -> Unit,
    onPreparation: (String) -> Unit,
    onReceipt: (String) -> Unit,
    onStart: () -> Unit,
    onCopy: (Stop) -> String = { "" },
    navigationOpen: Boolean = false,
    routeMap: RouteMap = MapLibreRouteMap,
    onProfile: () -> Unit = {},
    onStep: (DepartureStep) -> Unit = {}, onBackStep: () -> Unit = {},
    onCargoConfirmation: (Boolean) -> Unit = {}, onOpenIssue: () -> Unit = {},
    onDismissIssue: () -> Unit = {}, onEditIssue: (String?, CargoIssueReason?, String) -> Unit = { _, _, _ -> },
    onSubmitIssue: () -> Unit = {}, onResolveIssue: (String) -> Unit = {}, onReceive: () -> Unit = {}
) {
    val detailsStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    Box(Modifier.fillMaxSize().then(if (state.detailsOpen) Modifier.clearAndSetSemantics {} else Modifier)) {
        when {
            state.loading && state.route == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            state.route == null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Rotas", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                DemoBanner()
                Icon(NavigationIcons.Routes, null, tint = MaterialTheme.colorScheme.primary)
                Heading("Nenhuma rota atribuída")
                Text("Quando a operação atribuir uma rota, ela aparecerá aqui. Por enquanto, você está em um cenário demonstrativo.")
                Text("Você pode explorar os outros cenários na aba Perfil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction("Explorar cenários no Perfil", onProfile)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            state.route.status == RouteStatus.ACTIVE -> ActiveRoute(state, onSelectStop, onDetailsChange, onShowStops, onNextStop, routeMap)
            else -> {
                DepartureFlow(state, onAccept, onShowStops, onStart, onStep, onBackStep, onCargoConfirmation, onOpenIssue, onDismissIssue, onEditIssue, onSubmitIssue, onResolveIssue, onReceive, routeMap = routeMap)
            }
        }
    }
    if (state.detailsOpen && state.route != null) state.selectedStop?.let { stop ->
        detailsStates.SaveableStateProvider("${state.route.id}/${stop.id}") {
            StopDetailsLayer(state.route, stop, { onDetailsChange(false) }, onNavigate, onCall, onCopy, navigationOpen)
        }
    }
}

@Composable
fun StopsScreen(route: Route?, onSelectStop: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackAction("Voltar para a rota", onBack)
        Heading("Lista de paradas")
        DemoBanner()
        if (route == null) Text("Esta rota não está disponível.") else {
            Text("${route.code} • ${route.stops.size} paradas na ordem da operação")
            route.stops.sortedBy { it.sequence }.forEach { stop ->
                SectionCard(onClick = { onSelectStop(stop.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("${stop.sequence}. ${stop.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${stop.type.label} • ${stop.status.label}${if (stop.id == route.nextStop?.id) " • Próxima parada" else ""}", color = MaterialTheme.colorScheme.primary)
                        if (stop.id == route.nextStop?.id) StatusBadge("Próxima parada")
                        Text(stop.address)
                        Text("${stop.window} • ${stop.volumes} volumes")
                        Text("Ver detalhes", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun Detail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun Heading(text: String) = Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

@Composable
internal fun SyncNotice(status: SyncStatus) {
    Text("Envio simulado: ${status.label}. Altere a simulação no Perfil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
