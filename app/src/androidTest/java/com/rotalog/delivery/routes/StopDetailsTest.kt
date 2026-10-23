package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.model.Stop
import com.rotalog.delivery.ui.theme.RotalogTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StopDetailsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun externalFeedbackIsVisibleOnceInTheRelevantCard() {
        val route = DemoRoutes.active()
        compose.setContent {
            RotalogTheme {
                RoutesScreen(RoutesUiState(route = route, selectedStopId = route.nextStop!!.id, detailsOpen = true, loading = false),
                    {}, {}, {}, {}, {}, { "Discador indisponível neste aparelho." }, {}, {}, {}, {}, { "Endereço copiado." })
            }
        }
        compose.onNodeWithText("Copiar endereço").performScrollTo().performClick()
        compose.onNodeWithText("Endereço copiado.").assertIsDisplayed()
        compose.onNodeWithText("Ligar para o contato").performScrollTo().performClick()
        compose.onAllNodesWithText("Discador indisponível neste aparelho.").assertCountEquals(1)
        compose.onNodeWithText("Discador indisponível neste aparelho.").assertIsDisplayed()
    }

    @Test fun actionsAdaptToWindowAndMissingContactAndProductsRemainReadable() {
        val source = DemoRoutes.active()
        val stop = source.nextStop!!.copy(name = "Cliente com nome completo extenso para consulta", contact = "", phone = "", items = emptyList(),
            instructions = "Instruções completas para entrega. ".repeat(20))
        val route = source.copy(stops = listOf(stop))
        compose.setContent {
            RotalogTheme {
                RoutesScreen(RoutesUiState(route = route, selectedStopId = stop.id, detailsOpen = true, loading = false),
                    {}, {}, {}, {}, {}, { null }, {}, {}, {}, {}, { "Endereço copiado." })
            }
        }
        compose.onNodeWithText("Copiar endereço").performScrollTo()
        val navigate = compose.onNodeWithText("Navegar").getUnclippedBoundsInRoot()
        val copy = compose.onNodeWithText("Copiar endereço").getUnclippedBoundsInRoot()
        val configuration = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
        if (configuration.screenWidthDp < 360 || configuration.fontScale >= 1.5f) assertTrue(copy.top >= navigate.bottom)
        else assertTrue(copy.left >= navigate.right)
        compose.onNodeWithText("Ligar para o contato").assertDoesNotExist()
        compose.onNodeWithText(stop.instructions).performScrollTo().assertExists()
        compose.onNodeWithText("Nenhum produto previsto nesta parada.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Envio simulado:", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun mapViewportRemainsMountedWhileModalHidesBackgroundAndRestoresPerStopScroll() {
        val route = DemoRoutes.active()
        var state by mutableStateOf(RoutesUiState(route = route, selectedStopId = route.nextStop!!.id, loading = false))
        var viewport: Pair<Dp, Dp>? = null
        var mapSize = IntSize.Zero
        var disposed = false
        val renderer = object : RouteMap {
            @Composable override fun Content(stops: List<Stop>, selectedStopId: String?, nextStopId: String?, topInset: Dp, bottomInset: Dp, onSelectStop: (String) -> Unit, modifier: Modifier) {
                viewport = topInset to bottomInset
                DisposableEffect(Unit) { onDispose { disposed = true } }
                Box(modifier.onSizeChanged { mapSize = it }) { Text("Mapa de teste") }
            }
        }
        compose.setContent {
            RotalogTheme {
                RoutesScreen(state, {}, {}, { state = state.copy(detailsOpen = it) }, {}, {}, { null }, {}, {}, {}, {}, routeMap = renderer)
            }
        }
        compose.waitForIdle()
        val previousViewport = viewport
        val previousSize = mapSize
        compose.onNodeWithText("Ver detalhes").performClick()
        compose.onNodeWithText("Mapa de teste").assertDoesNotExist()
        compose.onNodeWithText("Envio simulado:", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar para a rota").performClick()
        compose.onNodeWithText("Mapa de teste").assertExists()
        compose.runOnIdle { assertFalse(disposed); assertEquals(previousViewport, viewport); assertEquals(previousSize, mapSize) }
        compose.onNodeWithText("Ver detalhes").performClick()
        compose.onNodeWithText("Envio simulado:", substring = true).assertIsDisplayed()
    }
}
