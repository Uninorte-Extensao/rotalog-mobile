package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun StopDetailsLayer(route: Route, stop: Stop, onBack: () -> Unit,
    onNavigate: (Stop) -> Unit, onCall: (Stop) -> String?, onCopy: (Stop) -> String, navigationOpen: Boolean) {
    val scroll = rememberScrollState()
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var callFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = androidx.compose.ui.platform.LocalView.current
        val light = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
        }
        Surface(Modifier.fillMaxSize().then(if (navigationOpen) Modifier.clearAndSetSemantics {} else Modifier), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(AppIcons.Back, "Voltar para a rota") }
                    Text("Detalhes da parada", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                }
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${stop.sequence}. ${stop.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    StatusBadge("${stop.type.label} • ${stop.status.label}")
                    SectionCard {
                        Icon(AppIcons.Pin, null, tint = MaterialTheme.colorScheme.primary)
                        Detail("Endereço completo", stop.address)
                        Detail("Referência", stop.reference)
                        Detail("Janela de atendimento", stop.window)
                        AdaptiveGroup {
                            Button(onClick = { onNavigate(stop) }, modifier = Modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.small) { Text("Navegar") }
                            OutlinedButton(onClick = { feedback = onCopy(stop) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Copiar endereço") }
                        }
                        ActionFeedback(feedback)
                    }
                    if (stop.contact.isNotBlank() || stop.phone.isNotBlank()) SectionCard {
                        Icon(AppIcons.Phone, null, tint = MaterialTheme.colorScheme.primary)
                        Detail("Contato", listOf(stop.contact, stop.phone).filter { it.isNotBlank() }.joinToString("\n"))
                        if (stop.phone.isNotBlank()) OutlinedButton(onClick = { callFeedback = onCall(stop) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ligar para o contato") }
                        ActionFeedback(callFeedback, error = true)
                    }
                    SectionCard { Detail("Instruções", stop.instructions) }
                    SectionCard {
                        Text("Produtos e quantidades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (stop.items.isEmpty()) Text("Nenhum produto previsto nesta parada.")
                        stop.items.forEach { item ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(item.description, fontWeight = FontWeight.SemiBold)
                                    Text("${item.quantity} ${item.unit}")
                                }
                            }
                        }
                        Text("${stop.volumes} volumes ${if (stop.type == StopType.COLLECTION) "para coleta" else if (stop.type == StopType.DELIVERY) "para entrega" else "previstos"}", fontWeight = FontWeight.SemiBold)
                    }
                    SectionCard {
                        Text("Carga da rota", style = MaterialTheme.typography.titleMedium)
                        Text("Coletados: ${route.collectedVolumes} / ${route.collectionVolumes} volumes\nEntregues: ${route.deliveredVolumes} / ${route.deliveryVolumes} volumes")
                        SyncNotice(route.syncStatus)
                    }
                }
            }
        }
    }
}
