package com.rotalog.delivery.loading

import com.rotalog.delivery.shared.captureScrollPages
import com.rotalog.delivery.shared.captureFrame
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container get() = (compose.activity.application as RotalogApplication).container
    private fun click(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
    private fun scrollClick(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()
    @Before fun reset() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) } }
    private fun prepare() {
        scrollClick("Aceitar rota"); click("Confirmar aceite")
        scrollClick("Preparar saída")
        compose.onNodeWithText("Confirmar checklist").performScrollTo().assertIsNotEnabled()
        PreparationCheck.entries.forEach { scrollClick(it.label) }
        scrollClick("Confirmar checklist")
        compose.runOnIdle {
            assertTrue(container.routes.route.value!!.prepared)
            assertFalse(container.routes.route.value!!.cargoReceived)
        }
        scrollClick("Avançar para conferência")
    }
    private fun capture(name: String) {
        compose.waitForIdle(); SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val scale = (context.resources.configuration.fontScale * 100).toInt()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name-$scale.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/RotaLogUnifiedReview")
        }
        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(context.contentResolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun discrepancyBlocksReceiptThenExplicitStartUsesFirstPendingDelivery() {
        prepare()
        scrollClick("Conferir Óleo")
        compose.onAllNodesWithText("Relatar divergência")[1].performScrollTo().performClick()
        compose.onNodeWithText("Embalagem avariada").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Caixa amassada na doca")
        closeSoftKeyboard()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertTextContains("Caixa amassada na doca")
        click("Salvar divergência")
        compose.onNodeWithText("Confirmar recebimento da carga").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, container.routes.route.value!!.openCargoIssues.size)
            assertFalse("oleo" in container.routes.route.value!!.checkedCargo)
        }
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        scrollClick("Registrar resolução no CD")
        click("Confirmar resolução")
        compose.onNodeWithText("Aguardando nova conferência").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Conferir Óleo").performScrollTo().assertIsOff()
        val manifest = container.routes.route.value!!.manifest
        manifest.forEach { scrollClick("Conferir ${it.description}") }
        scrollClick("Confirmar recebimento da carga")
        click("Cancelar")
        compose.runOnIdle { assertFalse(container.routes.route.value!!.cargoReceived) }
        scrollClick("Confirmar recebimento da carga"); click("Confirmar carga recebida")
        compose.runOnIdle {
            assertTrue(container.routes.route.value!!.cargoReceived)
            assertEquals(RouteStatus.ACCEPTED, container.routes.route.value!!.status)
            assertEquals(0, container.routes.route.value!!.deliveredVolumes)
        }
        scrollClick("Voltar à rota para iniciar")
        scrollClick("Iniciar rota"); click("Cancelar")
        compose.runOnIdle { assertEquals(RouteStatus.ACCEPTED, container.routes.route.value!!.status) }
        scrollClick("Iniciar rota"); click("Iniciar agora")
        compose.onNodeWithContentDescription("Parada 2, Drogaria Rio Negro, próxima parada, selecionada").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(RouteStatus.ACTIVE, container.routes.route.value!!.status)
            assertEquals(1, container.routes.route.value!!.completedStops)
        }
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithContentDescription("Parada 2, Drogaria Rio Negro, próxima parada, selecionada").assertIsDisplayed()
    }
    @Test fun loadingScreensFollowBothThemesAndRestoreChecklistDraft() {
        for (theme in listOf("Claro", "Escuro")) {
            kotlinx.coroutines.runBlocking {
                container.settings.setTheme(if (theme == "Claro") ThemeMode.LIGHT else ThemeMode.DARK)
            }
            compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
            scrollClick("Aceitar rota")
            captureFrame(compose, "$theme-accept-confirmation")
            click("Confirmar aceite")
            captureScrollPages(compose, "$theme-accepted")
            scrollClick("Preparar saída")
            scrollClick(PreparationCheck.FUEL.label)
            compose.activityRule.scenario.recreate(); compose.waitForIdle()
            compose.onNodeWithText(PreparationCheck.FUEL.label).performScrollTo().assertIsOn()
            captureScrollPages(compose, "$theme-checklist")
            PreparationCheck.entries.filterNot { it == PreparationCheck.FUEL }.forEach { scrollClick(it.label) }
            scrollClick("Confirmar checklist"); scrollClick("Avançar para conferência")
            compose.onNodeWithText("Produtos de higiene").performScrollTo().assertIsDisplayed()
            captureScrollPages(compose, "$theme-conferencia")
            compose.onAllNodesWithText("Relatar divergência")[0].performScrollTo().performClick()
            capture("$theme-divergencia")
            click("Cancelar")
            container.routes.route.value!!.manifest.forEach { scrollClick("Conferir ${it.description}") }
            scrollClick("Confirmar recebimento da carga"); click("Confirmar carga recebida")
            compose.onNodeWithText("Carga recebida confirmada").performScrollTo().assertIsDisplayed()
            captureScrollPages(compose, "$theme-recebimento")
            scrollClick("Voltar à rota para iniciar")
            captureScrollPages(compose, "$theme-ready-to-start")
            scrollClick("Iniciar rota")
            captureFrame(compose, "$theme-start-confirmation")
            click("Cancelar")
        }
    }
}
