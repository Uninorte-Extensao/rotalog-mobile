package com.rotalog.delivery.routes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.*

@Composable
internal fun DepartureFlow(
    state: RoutesUiState, onAccept: () -> Unit, onShowStops: (String) -> Unit,
    onStart: () -> Unit, onStep: (DepartureStep) -> Unit, onBack: () -> Unit,
    onCheck: (Boolean) -> Unit, onOpenIssue: () -> Unit, onDismissIssue: () -> Unit,
    onEditIssue: (String?, CargoIssueReason?, String) -> Unit, onSubmitIssue: () -> Unit,
    onResolve: (String) -> Unit, onReceive: () -> Unit,
    routeMap: RouteMap = MapLibreRouteMap
) {
    val route = state.route ?: return
    var confirmation by rememberSaveable(route.id) { mutableStateOf<String?>(null) }
    var resolveProduct by rememberSaveable(route.id) { mutableStateOf<String?>(null) }
    var originOpen by rememberSaveable(route.id) { mutableStateOf(false) }
    val step = state.departureStep
    BackHandler(enabled = step != DepartureStep.ACCEPTANCE && confirmation == null && resolveProduct == null && !originOpen && !state.issueDraft.open && !state.detailsOpen) { onBack() }
    key(route.id, step) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step != DepartureStep.ACCEPTANCE) TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Voltar") }
                Column(Modifier.weight(1f)) {
                    Text(route.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Etapa ${step.ordinal + 1} de 3 • ${step.label}", style = MaterialTheme.typography.labelMedium)
                }
            }
            DepartureIndicator(state, onStep)
            when (step) {
                DepartureStep.ACCEPTANCE -> {
                    Text("Sua próxima saída", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    DemoBanner()
                    MapCourse(routeMap, route)
                    SectionCard {
                        Text(route.date, style = MaterialTheme.typography.titleMedium)
                        Text("${route.stops.size} paradas • ${route.deliveryVolumes} volumes para entrega")
                        val origin = route.stops.firstOrNull { it.type == StopType.COLLECTION }
                        Text("Origem: ${origin?.name.orEmpty()}")
                        route.stops.firstOrNull { it.type == StopType.RETURN }?.let { Text("Retorno: ${it.name}") }
                        Text("Renault Master • PHB-9A23")
                        OutlinedButton(onClick = { originOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver dados completos") }
                        OutlinedButton(onClick = { onShowStops(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver lista de paradas") }
                    }
                    if (route.status == RouteStatus.ASSIGNED) PrimaryAction("Aceitar rota", { confirmation = "accept" }, enabled = !state.loading)
                    else {
                        StatusBadge("Aceite confirmado")
                        PrimaryAction("Continuar para recebimento", { onStep(DepartureStep.RECEIPT) })
                    }
                }
                DepartureStep.RECEIPT -> {
                    val origin = route.stops.firstOrNull { it.type == StopType.COLLECTION }
                    Text("${origin?.name.orEmpty()} • ${origin?.reference.orEmpty()}", style = MaterialTheme.typography.labelMedium)
                    Text("${route.manifest.size} mercadorias • ${route.collectionVolumes} volumes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Manifest(route)
                    TextButton(onClick = { originOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver dados da origem") }
                    if (route.cargoReceived) StatusBadge("Carga recebida confirmada")
                    route.cargoIssues.forEach { issue ->
                        SectionCard {
                            Text(route.manifest.find { it.productId == issue.productId }?.description.orEmpty(), fontWeight = FontWeight.Bold)
                            Text(if (issue.resolved) "Resolução registrada • confira novamente a carga, incluindo este produto." else "⚠ Divergência pendente", color = if (issue.resolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(issue.reason.label)
                            if (issue.note.isNotBlank()) Text(issue.note)
                            if (!issue.resolved && !route.cargoReceived) OutlinedButton(onClick = { resolveProduct = issue.productId }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Registrar resolução") }
                        }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(state.cargoConfirmed, enabled = !state.loading && !route.cargoReceived && route.openCargoIssues.isEmpty(), role = Role.Checkbox, onValueChange = onCheck), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(state.cargoConfirmed, onCheckedChange = null, enabled = !state.loading && !route.cargoReceived && route.openCargoIssues.isEmpty())
                        Text("Conferi as mercadorias e confirmo que os ${route.collectionVolumes} volumes estão corretos.", Modifier.weight(1f))
                    }
                    if (!route.cargoReceived) {
                        if (route.openCargoIssues.isNotEmpty()) Text("Resolva as divergências e confira novamente toda a carga.", color = MaterialTheme.colorScheme.error)
                        PrimaryAction("Confirmar recebimento", { confirmation = "receive" }, enabled = !state.loading && route.canReceive)
                        OutlinedButton(onClick = onOpenIssue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.loading) { Text("Indicar divergência") }
                    } else PrimaryAction("Continuar para início", { onStep(DepartureStep.START) })
                    Text("Demonstração • registro local, sem envio ao CD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DepartureStep.START -> {
                    SectionCard {
                        StatusBadge("Carga recebida confirmada")
                        Text("${route.collectionVolumes} volumes • ${route.stops.size} paradas", style = MaterialTheme.typography.titleLarge)
                        Text("${route.date} • ${route.code}")
                        Text("A partida exige sua confirmação. O mapa demonstrativo abre após iniciar a rota.")
                    }
                    PrimaryAction("Iniciar rota", { confirmation = "start" }, enabled = !state.loading && route.canStart)
                    DemoBanner()
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (originOpen) PartialDialog({ originOpen = false }) {
        Text("Dados da rota", style = MaterialTheme.typography.titleLarge)
        route.stops.filter { it.type != StopType.DELIVERY }.forEach { stop ->
            Text(stop.name, fontWeight = FontWeight.Bold); Text(stop.address); Text(stop.reference); Text(stop.window); Text(stop.instructions)
        }
        TextButton(onClick = { originOpen = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fechar") }
    }
    if (confirmation != null) {
        val action = confirmation
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(when (action) { "accept" -> "Aceitar ${route.code}?"; "receive" -> "Confirmar recebimento?"; else -> "Iniciar rota demonstrativa?" }) },
            text = { Text(when (action) {
                "accept" -> "O aceite confirma a atribuição. Depois, confira e receba a carga antes de iniciar."
                "receive" -> "Todos os ${route.collectionVolumes} volumes foram conferidos e estão corretos? Confirmar registra a coleta nesta demonstração. A partida continua sendo uma ação separada."
                else -> "A carga foi recebida. O mapa abrirá na primeira entrega pendente. Esta demonstração não ativa GPS nem envia dados ao servidor."
            }, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { confirmation = null; when (action) { "accept" -> onAccept(); "receive" -> onReceive(); else -> onStart() } }, modifier = Modifier.heightIn(min = 48.dp)) { Text(when (action) { "accept" -> "Confirmar aceite"; "receive" -> "Confirmar carga recebida"; else -> "Iniciar agora" }) } },
            dismissButton = { TextButton(onClick = { confirmation = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (action == "accept") "Voltar" else "Cancelar") } })
    }
    if (resolveProduct != null) AlertDialog(onDismissRequest = { resolveProduct = null }, title = { Text("Divergência resolvida?") },
        text = { Text("Confirme somente após resolver com a equipe do CD. Confira novamente toda a carga, incluindo o produto afetado.", Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { resolveProduct?.let(onResolve); resolveProduct = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Confirmar resolução") } },
        dismissButton = { TextButton(onClick = { resolveProduct = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") } })
    if (state.issueDraft.open) CargoIssueDialog(route, state.issueDraft, onDismissIssue, onEditIssue, onSubmitIssue)
}

@Composable
private fun DepartureIndicator(state: RoutesUiState, onStep: (DepartureStep) -> Unit) {
    val route = state.route ?: return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        DepartureStep.entries.forEachIndexed { index, step ->
            val complete = when (step) { DepartureStep.ACCEPTANCE -> route.status != RouteStatus.ASSIGNED; DepartureStep.RECEIPT -> route.cargoReceived; DepartureStep.START -> route.status == RouteStatus.ACTIVE }
            val operational = when { route.cargoReceived -> DepartureStep.START; route.status != RouteStatus.ASSIGNED -> DepartureStep.RECEIPT; else -> DepartureStep.ACCEPTANCE }
            Column(Modifier.weight(1f).selectable(selected = state.departureStep == step, enabled = state.canView(step), role = Role.Tab, onClick = { onStep(step) }).heightIn(min = 48.dp).semantics { contentDescription = "Etapa ${index + 1}, ${step.label}${if (complete) ", confirmada" else if (step == operational) ", atual" else ", pendente"}" }, horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = if (complete) MaterialTheme.colorScheme.primary else if (step == operational) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (complete) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (state.departureStep == step) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null) {
                    Box(Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp).padding(4.dp), contentAlignment = Alignment.Center) { Text("${index + 1}", fontWeight = FontWeight.Bold) }
                }
                Text(step.label, style = MaterialTheme.typography.labelMedium)
            }
            if (index < 2) HorizontalDivider(Modifier.width(16.dp).padding(top = 16.dp), color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
        }
    }
}

@Composable
private fun Manifest(route: Route) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mercadoria", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Quantidade", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            route.manifest.forEach { item ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        route.cargoIssues.find { it.productId == item.productId }?.let { Text(if (it.resolved) "✓ Resolução registrada" else "⚠ Divergência", style = MaterialTheme.typography.labelSmall, color = if (it.resolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                    }
                    Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PartialDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(.55f)
                setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        val height = LocalConfiguration.current.screenHeightDp.dp * .85f
        // This dialog window already resizes for the keyboard; a second IME inset hides content.
        Surface(Modifier.padding(horizontal = 20.dp).widthIn(max = 420.dp).fillMaxWidth().heightIn(max = height), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun CargoIssueDialog(route: Route, draft: CargoIssueDraft, onDismiss: () -> Unit, onEdit: (String?, CargoIssueReason?, String) -> Unit, onSubmit: () -> Unit) {
    var productExpanded by remember { mutableStateOf(false) }
    var reasonExpanded by remember { mutableStateOf(false) }
    PartialDialog(onDismiss) {
        Text("Indicar divergência", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Registro demonstrativo, sem envio ao CD.", style = MaterialTheme.typography.bodySmall)
        Text("Produto *", style = MaterialTheme.typography.labelLarge)
        Box {
            val item = route.manifest.find { it.productId == draft.productId }
            OutlinedButton(onClick = { productExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !draft.saving) { Text(item?.let { "${it.description} • ${it.quantity} ${it.unit}" } ?: "Selecionar produto") }
            DropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                route.manifest.forEach { product -> DropdownMenuItem(text = { Text("${product.description} • ${product.quantity} ${product.unit}") }, onClick = { onEdit(product.productId, draft.reason, draft.description); productExpanded = false }) }
            }
        }
        Text("Motivo *", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { reasonExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !draft.saving) { Text(draft.reason?.label ?: "Selecionar motivo") }
            DropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                CargoIssueReason.entries.forEach { reason -> DropdownMenuItem(text = { Text(reason.label) }, onClick = { onEdit(draft.productId, reason, draft.description); reasonExpanded = false }) }
            }
        }
        OutlinedTextField(value = draft.description, onValueChange = { onEdit(draft.productId, draft.reason, it) }, label = { Text("Descrição (opcional)") }, supportingText = { Text("${draft.description.length}/500 caracteres") }, enabled = !draft.saving, modifier = Modifier.fillMaxWidth())
        draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryAction(if (draft.saving) "Registrando…" else "Registrar divergência", onSubmit, enabled = !draft.saving && draft.productId != null && draft.reason != null)
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !draft.saving) { Text("Cancelar") }
    }
}
