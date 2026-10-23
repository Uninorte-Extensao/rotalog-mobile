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
import androidx.compose.ui.draw.clip


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
    routeMap: RouteMap =MapLibreRouteMap ,
    onProfile: () -> Unit = {}
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
                AssignedRoute(state, onAccept, onShowStops, onPreparation, onReceipt, onStart, routeMap)

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
private fun AssignedRoute(state: RoutesUiState, onAccept: () -> Unit, onShowStops: (String) -> Unit, onPreparation: (String) -> Unit, onReceipt: (String) -> Unit, onStart: () -> Unit, routeMap: RouteMap) {
    val route = state.route ?: return
    var confirm by rememberSaveable { mutableStateOf(false) }
    var confirmStart by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Rotas", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(if (route.status == RouteStatus.ASSIGNED) "Confira sua próxima saída." else "Acompanhe as etapas antes da partida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        DemoBanner()
        MapaPercurso(routeMap, route)
        SectionCard {
            Text(route.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(AppIcons.Calendar, null); Text(route.date) }
            StatusBadge(route.status.label)
            AdaptiveGroup {
                Column { Text("${route.stops.size}", style = MaterialTheme.typography.headlineMedium); Text("Paradas") }
                Column { Text("${route.deliveryVolumes}", style = MaterialTheme.typography.headlineMedium); Text("Volumes para entrega") }
            }
            Text("${route.collectionVolumes} volumes previstos para coleta")
            HorizontalDivider()
            when {
                route.status == RouteStatus.ASSIGNED -> PrimaryAction("Aceitar rota", { confirm = true }, enabled = !state.loading)
                !route.prepared -> {
                    Text("Confirme o checklist para liberar a conferência e a partida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryAction(if (route.checklist.isNotEmpty() || route.cargoReceived) "Revisar checklist" else "Preparar saída", { onPreparation(route.id) }, enabled = !state.loading)
                }
                !route.cargoReceived -> {
                    Text("Confira todos os produtos e resolva as divergências antes de receber.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryAction("Conferir carga no CD", { onReceipt(route.id) }, enabled = !state.loading)
                    TextButton(onClick = { onPreparation(route.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Consultar checklist") }
                }
                else -> {
                    PrimaryAction("Iniciar rota", { confirmStart = true }, enabled = route.canStart && !state.loading)
                    TextButton(onClick = { onPreparation(route.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Consultar checklist") }
                }
            }
            if (route.cargoReceived) OutlinedButton(onClick = { onReceipt(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Revisar carga recebida") }
        }
        SectionCard {
            Text("Antes de sair", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val accepted = route.status != RouteStatus.ASSIGNED
            Stage("1", "Aceite", if (accepted) "Concluída" else "Atual", accepted)
            Stage("2", "Checklist", if (route.prepared) "Concluída" else if (accepted) "Atual" else "Pendente", route.prepared)
            Stage("3", "Conferência", if (route.cargoReceived) "Concluída" else if (route.prepared) "Atual" else "Pendente", route.cargoReceived)
            Stage("4", "Iniciar", if (route.canStart) "Atual" else "Pendente", false)
        }
        SectionCard {
            Text("Logística da rota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            route.stops.firstOrNull { it.type == StopType.COLLECTION }?.let { Detail("Origem • ${it.name}", it.address) }
            route.stops.firstOrNull { it.type == StopType.RETURN }?.let { Detail("Retorno • ${it.name}", it.address) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(AppIcons.Truck, null); Text("Renault Master • PHB-9A23") }
            OutlinedButton(onClick = { onShowStops(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver lista de paradas") }
        }
        SyncNotice(route.syncStatus)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
    }
    if (confirmStart) AlertDialog(
        onDismissRequest = { confirmStart = false }, title = { Text("Iniciar rota demonstrativa?") },
        text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = "A carga foi recebida e o checklist confirmado. O mapa abrirá na primeira entrega pendente. Esta demonstração usa o GPS só para mostrar sua posição no mapa e não envia dados ao servidor.") },
        confirmButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmStart = false; onStart() }) { Text("Iniciar agora") } },
        dismissButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmStart = false }) { Text("Cancelar") } }
    )
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Aceitar ${route.code}?") },
        text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = "O aceite confirma a atribuição desta rota. Você ainda precisará receber a carga e preparar a saída.") },
        confirmButton = { TextButton(onClick = { confirm = false; onAccept() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Confirmar aceite") } },
        dismissButton = { TextButton(onClick = { confirm = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Voltar") } }
    )
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
private fun Stage(number: String, title: String, subtitle: String, complete: Boolean) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (complete) Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$number. $title", fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
