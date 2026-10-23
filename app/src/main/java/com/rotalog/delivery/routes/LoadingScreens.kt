package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.*

@Composable
fun PreparationScreen(
    state: RoutesUiState, onCheck: (PreparationCheck, Boolean) -> Unit,
    onConfirm: () -> Unit, onContinue: () -> Unit, onBack: () -> Unit,
    routeMap: RouteMap = MapLibreRouteMap
) {
    val route = state.route
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackAction("Voltar à rota", onBack)
        PageTitle("Checklist antes de sair", "Verifique as condições do veículo antes de conferir a carga.")
        DemoBanner()
        if (route == null || route.status != RouteStatus.ACCEPTED) {
            Text("Aceite uma rota atribuída para preparar a saída.")
            return@Column
        }

        MapaPercurso(routeMap, route)

        SectionCard {
            Text(route.code, style = MaterialTheme.typography.titleLarge)
            Text("Etapa 2 de 4 • Checklist inicial")
        }
        SectionCard {
            Icon(AppIcons.Truck, null, tint = MaterialTheme.colorScheme.primary)
            Text("Renault Master", style = MaterialTheme.typography.titleLarge)
            StatusBadge("PHB-9A23")
        }
        SectionCard {
            Text("Condições para saída", style = MaterialTheme.typography.titleMedium)
            Text("${route.checklist.size} de ${PreparationCheck.entries.size} itens verificados", color = MaterialTheme.colorScheme.primary)
            PreparationCheck.entries.forEach { item ->
                CheckRow(item.label, item in route.checklist, enabled = !state.loading) { onCheck(item, it) }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (route.prepared) {
            StatusBadge("Checklist confirmado")
            Text("O recebimento é confirmado separadamente, após a conferência física no CD.")
            PrimaryAction("Avançar para conferência", onContinue)
        } else {
            Text("Verifique os quatro itens para confirmar o preparo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryAction("Confirmar checklist", onConfirm, enabled = !state.loading && route.checklist.containsAll(PreparationCheck.entries))
        }
        Text("Ao desmarcar um item, o preparo volta a ficar pendente. O recebimento já confirmado permanece registrado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReceiptScreen(
    state: RoutesUiState, onCheck: (String, Boolean) -> Unit,
    onReport: (CargoIssue) -> Unit, onResolve: (String) -> Unit,
    onConfirm: () -> Unit, onBack: () -> Unit, onPreparation: () -> Unit
) {
    val route = state.route
    var reportProduct by rememberSaveable { mutableStateOf<String?>(null) }
    var resolveProduct by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReceipt by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackAction("Voltar à rota", onBack)
        PageTitle("Recebimento da carga", "Conferência física na saída do CD")
        DemoBanner("Demonstração • registro local, sem envio ao CD")
        if (route == null || route.status != RouteStatus.ACCEPTED) {
            Text("Esta rota não está disponível para recebimento.")
            return@Column
        }
        LoadingHeader(route, if (route.cargoReceived) 4 else 3, if (route.canStart) "Pronto para partida" else if (route.cargoReceived) "Carga recebida • checklist pendente" else "Conferência na doca")
        if (!route.prepared) {
            SectionCard {
                Text("Checklist pendente", style = MaterialTheme.typography.titleMedium)
                Text("Confirme as condições do veículo antes de conferir a carga.")
                PrimaryAction("Revisar checklist", onPreparation)
            }
        }
        if (route.cargoReceived) {
            SectionCard {
                StatusBadge("Carga recebida confirmada")
                Text("${route.collectionVolumes} volumes conferidos no CD.", style = MaterialTheme.typography.titleLarge)
                Text("A coleta está concluída. A rota só começa após a ação Iniciar rota, na tela Rotas. Esta demonstração não coleta GPS nem gera assinatura digital.")
                PrimaryAction("Voltar à rota para iniciar", onBack)
            }
        }
        Text("Volumes previstos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${route.checkedCargo.size} de ${route.manifest.size} produtos conferidos • ${route.collectionVolumes} volumes")
        LinearProgressIndicator(progress = { if (route.manifest.isEmpty()) 0f else route.checkedCargo.size.toFloat() / route.manifest.size }, modifier = Modifier.fillMaxWidth())
        route.manifest.forEach { item ->
            SectionCard {
                Text(item.description, style = MaterialTheme.typography.titleMedium)
                Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.titleLarge)
                val issue = route.cargoIssues.find { it.productId == item.productId }
                StatusBadge(when {
                    issue != null && !issue.resolved -> "⚠ Divergência pendente"
                    item.productId in route.checkedCargo -> "✓ Conferido"
                    issue?.resolved == true -> "Aguardando nova conferência"
                    else -> "○ Pendente de conferência"
                })
                CheckRow("Conferir ${item.description}", item.productId in route.checkedCargo,
                    enabled = !state.loading && route.prepared && !route.cargoReceived && route.openCargoIssues.none { it.productId == item.productId }
                ) { onCheck(item.productId, it) }
                if (route.prepared && !route.cargoReceived) {
                    TextButton(onClick = { reportProduct = item.productId }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Relatar divergência") }
                }
                route.cargoIssues.find { it.productId == item.productId }?.let { issue ->
                    Text(if (issue.resolved) "Divergência resolvida no CD • registro demonstrativo" else "Divergência pendente", color = if (issue.resolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text(issue.reason.label)
                    if (issue.note.isNotBlank()) Text(issue.note)
                    if (!issue.resolved && !route.cargoReceived) {
                        OutlinedButton(onClick = { resolveProduct = item.productId }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Registrar resolução no CD") }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!route.cargoReceived) {
            if (route.openCargoIssues.isNotEmpty()) Text("Recebimento bloqueado: resolva as divergências e confira novamente os produtos.", color = MaterialTheme.colorScheme.error)
            if (!route.prepared) Text("Recebimento bloqueado: confirme o checklist.", color = MaterialTheme.colorScheme.error)
            if (route.checkedCargo.size < route.manifest.size) Text("Recebimento bloqueado: confira todos os produtos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Marque cada produto somente após conferir todos os volumes físicos. Nenhum item é confirmado automaticamente.", style = MaterialTheme.typography.bodyMedium)
            PrimaryAction("Confirmar recebimento da carga", { confirmReceipt = true }, enabled = !state.loading && route.canReceive)
        }
    }
    val item = route?.manifest?.find { it.productId == reportProduct }
    if (item != null) CargoIssueDialog(item, { reportProduct = null }) { issue -> onReport(issue); reportProduct = null }
    if (resolveProduct != null) AlertDialog(
        onDismissRequest = { resolveProduct = null }, title = { Text("Divergência resolvida?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            route?.manifest?.find { it.productId == resolveProduct }?.let { Text(it.description, style = MaterialTheme.typography.titleMedium) }
            Text("Confirme apenas após resolver a falta ou avaria com a equipe do CD. O produto deverá ser conferido novamente. Este registro fica somente na demonstração.") } },
        confirmButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { resolveProduct?.let(onResolve); resolveProduct = null }) { Text("Confirmar resolução") } },
        dismissButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { resolveProduct = null }) { Text("Cancelar") } }
    )
    if (confirmReceipt && route != null) AlertDialog(
        onDismissRequest = { confirmReceipt = false }, title = { Text("Confirmar recebimento?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("Você confirma a conferência física dos ${route.collectionVolumes} volumes? Isso registra a coleta nesta demonstração e mantém a rota aguardando início.") } },
        confirmButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmReceipt = false; onConfirm() }) { Text("Confirmar carga recebida") } },
        dismissButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmReceipt = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun LoadingHeader(route: Route, step: Int, label: String) {
    SectionCard {
        Text(route.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(route.date)
        StatusBadge("${route.collectionVolumes} volumes")
        val origin = route.stops.firstOrNull { it.type == StopType.COLLECTION }
        origin?.let {
            Text(it.name, style = MaterialTheme.typography.titleMedium)
            Text(it.address, style = MaterialTheme.typography.bodyMedium)
            Text(it.reference, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Text("Etapa $step de 4 • $label", color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { (step - 1) / 4f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(text, Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CargoIssueDialog(item: StopItem, onDismiss: () -> Unit, onConfirm: (CargoIssue) -> Unit) {
    var reasonName by rememberSaveable(item.productId) { mutableStateOf(CargoIssueReason.MISSING.name) }
    var note by rememberSaveable(item.productId) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Registrar divergência") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${item.description} • ${item.quantity} ${item.unit}", style = MaterialTheme.typography.titleMedium)
                Text("Informe a equipe do CD. Este registro não envia uma notificação ou foto.")
                CargoIssueReason.entries.forEach { reason ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = reasonName == reason.name, role = Role.RadioButton, onClick = { reasonName = reason.name }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(reasonName == reason.name, onClick = null)
                        Text(reason.label, Modifier.weight(1f))
                    }
                }
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 500) note = it }, label = { Text("Observações (opcional)") }, supportingText = { Text("${note.length}/500 caracteres") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { onConfirm(CargoIssue(item.productId, CargoIssueReason.valueOf(reasonName), note)) }) { Text("Salvar divergência") } },
        dismissButton = { TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = onDismiss) { Text("Cancelar") } }
    )
}
