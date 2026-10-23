package com.rotalog.delivery.profile

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Overlapping real device frames cover the full scrollable profile at the device font scale. */
@RunWith(AndroidJUnit4::class)
class ProfileReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
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

    private fun capturePage(prefix: String) {
        compose.onNodeWithText("Carlos Eduardo Mendes").performScrollTo()
        val scroll = compose.onNode(hasScrollAction())
        scroll.performScrollToNode(hasText("Carlos Eduardo Mendes"))
        // Go all the way to the header, then capture overlapping viewports through the footer.
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100000f) }
        var page = 0
        do {
            capture("$prefix-${page++}")
            val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            if (range.value() >= range.maxValue()) break
            val step = scroll.fetchSemanticsNode().boundsInRoot.height * 0.65f
            scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, step) }
            check(page < 80) { "Profile scrolling did not reach the footer" }
        } while (true)
        compose.onNodeWithText("RotaLog Entregador • demonstração nativa").assertIsDisplayed()
    }
    @Test fun captureFullProfileInBothThemesCollapsedAndExpanded() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick()
        for (theme in listOf("Claro", "Escuro")) {
            compose.onNodeWithText(theme).performScrollTo().performClick()
            val prefix = if (theme == "Claro") "light" else "dark"
            capturePage("$prefix-collapsed")
            compose.onNodeWithText("Ambiente de demonstração").performScrollTo().performClick()
            capturePage("$prefix-expanded")
            compose.onNodeWithText("Conflito").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Ambiente de demonstração").performScrollTo().performClick()
        }
    }
}
