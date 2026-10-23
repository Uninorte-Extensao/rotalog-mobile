package com.rotalog.delivery.routes

import com.rotalog.delivery.shared.captureScrollPages
import com.rotalog.delivery.shared.captureFrame
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.DemoScenario
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Captures real device frames for manual review, including system bars and accessibility font scale. */
@RunWith(AndroidJUnit4::class)
class RouteReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNode(hasText(text) and hasClickAction()).performClick()
    private fun capture(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(100) // Let the device render thread present the settled frame.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scale = (context.resources.configuration.fontScale * 100).toInt()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name-$scale.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/RotaLogUnifiedReview")
        }
        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(context.contentResolver.openOutputStream(uri)).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    @Test fun reviewRoutesInBothThemesWithFullDetailsAndStops() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        // The preceding flow copies an address. Let Android's clipboard overlay dismiss.
        SystemClock.sleep(6_500)
        for (theme in listOf("Claro", "Escuro")) {
            val prefix = if (theme == "Claro") "light" else "dark"
            val container = (compose.activity.application as RotalogApplication).container
            kotlinx.coroutines.runBlocking {
                container.settings.setTheme(if (theme == "Claro") com.rotalog.delivery.model.ThemeMode.LIGHT else com.rotalog.delivery.model.ThemeMode.DARK)
            }
            compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
            captureScrollPages(compose, "$prefix-assigned")
            compose.onNodeWithText("Ver lista de paradas").performScrollTo().performClick()
            captureScrollPages(compose, "$prefix-stops")
            androidx.test.espresso.Espresso.pressBack()
            compose.runOnIdle { container.routes.selectScenario(DemoScenario.ACTIVE) }
            capture("$prefix-active")
            click("Ver detalhes")
            compose.onNodeWithText("Detalhes da parada").assertIsDisplayed()
            captureScrollPages(compose, "$prefix-details")
            compose.onNodeWithText("14 volumes para entrega").performScrollTo().assertIsDisplayed()
            capture("$prefix-products")
            androidx.test.espresso.Espresso.pressBack()
            compose.runOnIdle { container.routes.selectScenario(DemoScenario.EMPTY) }
            captureScrollPages(compose, "$prefix-empty")
        }
    }
}
