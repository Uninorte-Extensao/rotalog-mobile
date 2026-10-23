package com.rotalog.delivery.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.*

@Composable
fun HistoryScreen(history: List<Route>, onRoute: (String) -> Unit = {}) {
    val completed = history.filter { it.status == RouteStatus.COMPLETED }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Histórico", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Seu caminho, entrega por entrega.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        DemoBanner("Demonstração • histórico fictício. A rota atual aparece somente em Rotas.")
        val deliveries = completed.sumOf { route -> route.stops.count { it.type == StopType.DELIVERY && it.status == StopStatus.COMPLETED } }
        if (stackActions()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetric(completed.size.toString(), "Rotas concluídas")
                HistoryMetric(deliveries.toString(), "Entregas realizadas")
            }
        } else {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetric(completed.size.toString(), "Rotas concluídas", Modifier.weight(1f).fillMaxHeight())
                HistoryMetric(deliveries.toString(), "Entregas realizadas", Modifier.weight(1f).fillMaxHeight())
            }
        }
        Text("Rotas anteriores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (completed.isEmpty()) SectionCard {
            Icon(NavigationIcons.History, null)
            Text("Nenhuma rota concluída", style = MaterialTheme.typography.titleMedium)
            Text("Quando houver uma rota encerrada, ela aparecerá aqui.")
        }
        completed.forEach { route ->
            SectionCard {
                Text(route.date, style = MaterialTheme.typography.labelLarge)
                Text(route.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StatusBadge("Concluída")
                RouteSummary(route)
                OutlinedButton(onClick = { onRoute(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver paradas da rota") }
                Text("Registro demonstrativo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HistoryRouteScreen(route: Route?, onBack: () -> Unit) {
    var expanded by rememberSaveable(route?.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackAction("Voltar ao Histórico", onBack)
        if (route == null) {
            PageTitle("Rota do histórico indisponível", "Volte ao Histórico para consultar uma rota concluída.")
            return@Column
        }
        PageTitle(route.code, route.date)
        DemoBanner("Demonstração • consulta de rota concluída")
        SectionCard { StatusBadge("Concluída"); RouteSummary(route) }
        route.stops.sortedBy { it.sequence }.forEach { stop ->
            SectionCard(onClick = { expanded = if (expanded == stop.id) null else stop.id },
                modifier = Modifier.semantics { stateDescription = if (expanded == stop.id) "Expandido" else "Recolhido" }) {
                Text("${stop.sequence}. ${stop.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${stop.type.label} • ${stop.status.label} • ${stop.volumes} volumes")
                Text(if (expanded == stop.id) "Recolher parada" else "Consultar parada", color = MaterialTheme.colorScheme.primary)
                if (expanded == stop.id) {
                    HorizontalDivider()
                    Text(stop.address)
                    Text("Referência: ${stop.reference}")
                    Text("Janela: ${stop.window}")
                    Text(stop.instructions)
                    Text("Produtos e quantidades", style = MaterialTheme.typography.titleMedium)
                    if (stop.items.isEmpty()) Text("Nenhum produto previsto nesta parada.")
                    stop.items.forEach { Text("${it.description} • ${it.quantity} ${it.unit}") }
                }
            }
        }
    }
}

@Composable
private fun RouteSummary(route: Route) {
    Text("${route.completedStops} de ${route.stops.size} paradas concluídas")
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Carga coletada: ${route.collectedVolumes} volumes")
            Text("Carga entregue: ${route.deliveredVolumes} volumes")
        }
    }
}

@Composable
private fun HistoryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
