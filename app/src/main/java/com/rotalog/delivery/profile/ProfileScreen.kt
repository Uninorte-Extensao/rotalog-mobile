package com.rotalog.delivery.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.shared.AppIcons
import com.rotalog.delivery.model.DemoScenario
import com.rotalog.delivery.model.SyncStatus
import com.rotalog.delivery.model.ThemeMode
import com.rotalog.delivery.shared.SectionCard
import com.rotalog.delivery.shared.StatusBadge
import com.rotalog.delivery.ui.theme.RotaLogSpacing

@Composable
fun ProfileScreen(
    theme: ThemeMode,
    scenario: DemoScenario,
    syncStatus: SyncStatus,
    onThemeChange: (ThemeMode) -> Unit,
    onScenarioChange: (DemoScenario) -> Unit,
    onSyncStatusChange: (SyncStatus) -> Unit,
    onResetScenario: () -> Unit
) {
    var demoExpanded by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val stackThemes = windowWidth < 360.dp || density.fontScale >= 1.5f
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(RotaLogSpacing.page),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Perfil", modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Sua identidade e suas preferências.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProfileCard {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("CE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Carlos Eduardo Mendes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Entregador", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("MOT-7832", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StatusBadge("Identidade demonstrativa")
        }
        ProfileCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Truck, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Veículo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("Renault Master", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text("PHB-9A23", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        ProfileCard {
            SectionTitle("Aparência", "A preferência de tema é salva neste aparelho.")
            ThemeOptions(theme, stackThemes, onThemeChange)
        }
        ProfileCard {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { stateDescription = if (demoExpanded) "Expandido" else "Recolhido" }
                    .clickable(role = Role.Button, onClickLabel = if (demoExpanded) "Recolher demonstração" else "Expandir demonstração") { demoExpanded = !demoExpanded },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ambiente de demonstração", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (demoExpanded) ProfileIcons.Up else ProfileIcons.Down, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text("Dados fictícios, mapa sem GPS e estados de envio simulados.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Cenário atual: ${scenario.label}", style = MaterialTheme.typography.bodyMedium)
            Text("Envio simulado: ${syncStatus.label}", style = MaterialTheme.typography.bodyMedium)
            if (demoExpanded) {
                HorizontalDivider()
                SectionTitle("Cenário da rota", "O cenário ativo é um exemplo independente. Aceitar a rota atribuída mantém recebimento e preparação pendentes.")
                Column(Modifier.selectableGroup()) {
                    DemoScenario.entries.forEach { option -> ProfileChoice(option.label, option == scenario) { onScenarioChange(option) } }
                }
                Text("Os dados da operação ficam na memória desta demonstração. Somente o tema é mantido após fechar o aplicativo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onResetScenario, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = MaterialTheme.shapes.small) {
                    Text("Reiniciar cenário atual")
                }
                HorizontalDivider()
                SectionTitle("Estado de envio • simulado", "A escolha é manual. Não há envio ao servidor nem confirmação automática nesta entrega.")
                Column(Modifier.selectableGroup()) {
                    SyncStatus.entries.forEach { option -> ProfileChoice(option.label, option == syncStatus) { onSyncStatusChange(option) } }
                }
            }
        }
        Text("RotaLog Entregador • demonstração nativa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    SectionCard(content = content)
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ThemeOptions(theme: ThemeMode, stacked: Boolean, onThemeChange: (ThemeMode) -> Unit) {
    if (stacked) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                ThemeOption(mode, mode == theme, Modifier.fillMaxWidth()) { onThemeChange(mode) }
            }
        }
    } else {
        Row(Modifier.selectableGroup().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                ThemeOption(mode, mode == theme, Modifier.weight(1f).fillMaxHeight()) { onThemeChange(mode) }
            }
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(when (mode) { ThemeMode.SYSTEM -> ProfileIcons.Device; ThemeMode.LIGHT -> ProfileIcons.Sun; ThemeMode.DARK -> ProfileIcons.Moon }, null, Modifier.size(24.dp))
                if (selected) Icon(ProfileIcons.Check, "Tema selecionado", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(mode.label, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

private object ProfileIcons {
    private fun icon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, pathBuilder = draw)
        }.build()
    val Device = icon("Aparelho") {
        moveTo(6f, 2f); lineTo(18f, 2f); lineTo(18f, 22f); lineTo(6f, 22f); close()
        moveTo(10f, 18f); lineTo(14f, 18f)
    }
    val Sun = icon("Claro") {
        moveTo(16f, 12f); arcTo(4f, 4f, 0f, true, true, 8f, 12f); arcTo(4f, 4f, 0f, true, true, 16f, 12f)
        moveTo(12f, 1f); lineTo(12f, 5f); moveTo(12f, 19f); lineTo(12f, 23f)
        moveTo(1f, 12f); lineTo(5f, 12f); moveTo(19f, 12f); lineTo(23f, 12f)
        moveTo(4f, 4f); lineTo(7f, 7f); moveTo(17f, 17f); lineTo(20f, 20f)
        moveTo(4f, 20f); lineTo(7f, 17f); moveTo(17f, 7f); lineTo(20f, 4f)
    }
    val Moon = icon("Escuro") {
        moveTo(20f, 15f); curveTo(9f, 19f, 5f, 9f, 11f, 3f)
        curveTo(-2f, 5f, 2f, 25f, 15f, 21f); curveTo(18f, 20f, 20f, 17f, 20f, 15f)
    }
    val Check = icon("Seleção") { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) }
    val Down = icon("Expandir") { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }
    val Up = icon("Recolher") { moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f) }
}

@Composable
private fun ProfileChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
