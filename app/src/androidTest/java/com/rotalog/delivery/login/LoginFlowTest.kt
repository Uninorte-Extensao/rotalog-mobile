package com.rotalog.delivery.login

import com.rotalog.delivery.shared.captureScrollPages
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    private fun assertRenderedPassword(expected: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("login-password").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertEquals(expected, layouts.first().layoutInput.text.text)
    }

    @Test fun startsAtLoginAndValidatesWithoutAuthenticating() {
        compose.onNodeWithText("Entrar na sua conta").assertExists()
        compose.onNodeWithText("Rotas").assertDoesNotExist()
        click("Entrar")
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        compose.onNodeWithText("Digite sua senha.").assertExists()
        compose.onNodeWithTag("login-email").performScrollTo().performTextInput("nome@")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("segredo")
        closeSoftKeyboard()
        click("Entrar")
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        compose.onNodeWithTag("login-email").performScrollTo().performTextReplacement("entregador@exemplo.com")
        closeSoftKeyboard()
        compose.onNodeWithText("Informe um e-mail válido.").assertDoesNotExist()
        click("Entrar")
        compose.onNodeWithText("Autenticação indisponível nesta versão. Nenhum dado foi enviado.").assertExists()
        compose.onNodeWithText("Rotas").assertDoesNotExist()
    }

    @Test fun passwordVisibilityAndRecreationKeepCredentialsPrivate() {
        compose.onNodeWithTag("login-email").performScrollTo().performTextInput("entregador@exemplo.com")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("segredo")
        closeSoftKeyboard()
        compose.onNodeWithTag("login-password").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assertRenderedPassword("•".repeat(7))
        compose.onNodeWithContentDescription("Mostrar senha").performScrollTo().performClick()
        assertRenderedPassword("segredo")
        compose.onNodeWithContentDescription("Ocultar senha").performClick()
        assertRenderedPassword("•".repeat(7))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("login-email").assertTextContains("entregador@exemplo.com")
        compose.onNodeWithTag("login-password").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithContentDescription("Mostrar senha").performScrollTo().performClick()
        compose.onNodeWithTag("login-password").assertTextContains("segredo")
    }

    @Test fun recoveryIsHonestAndDemoRequiresSeparateAction() {
        click("Esqueci minha senha")
        compose.onNodeWithText("A recuperação de senha ainda não está conectada. Solicite ajuda à sua operação.").assertExists()
        compose.onNodeWithText("Rotas").assertDoesNotExist()
        click("Explorar demonstração")
        compose.onNode(hasText("Rotas") and hasClickAction()).assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Rotas") and hasClickAction()).assertExists()
    }
    @Test fun reviewLoginInBothThemesWithKeyboardAndErrors() {
        val settings = (compose.activity.application as RotalogApplication).container.settings
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            runBlocking { settings.setTheme(theme) }
            compose.waitForIdle()
            compose.onNodeWithText("Entrar na sua conta").performScrollTo().assertIsDisplayed()
            captureScrollPages(compose, "${theme.name}-login")
            compose.onNodeWithTag("login-email").performScrollTo().performTextReplacement("nome@exemplo.com")
            compose.onNodeWithTag("login-password").performScrollTo().performTextInput("segredo")
            capture("${theme.name}-keyboard")
            compose.onNodeWithTag("login-password").performImeAction()
            compose.onNodeWithText("Autenticação indisponível nesta versão. Nenhum dado foi enviado.").performScrollTo().assertIsDisplayed()
            capture("${theme.name}-unavailable")
            compose.onNodeWithTag("login-email").performScrollTo().performTextClearance()
            compose.onNodeWithTag("login-password").performScrollTo().performTextClearance()
            closeSoftKeyboard()
            click("Entrar")
            compose.onNodeWithText("Informe um e-mail válido.").performScrollTo().assertIsDisplayed()
            capture("${theme.name}-validation")
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val scale = (context.resources.configuration.fontScale * 100).toInt()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name-$scale.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/RotaLogUnifiedReview")
        }
        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        checkNotNull(context.contentResolver.openOutputStream(uri)).use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

}
