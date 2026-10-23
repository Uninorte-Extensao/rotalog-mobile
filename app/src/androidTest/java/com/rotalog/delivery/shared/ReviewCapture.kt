package com.rotalog.delivery.shared

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

/** Overlapping device frames retain system bars and cover the entire scrollable screen. */
fun captureScrollPages(compose: ComposeTestRule, prefix: String) {
    val scroll = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))[0]
    scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100000f) }
    var page = 0
    do {
        captureFrame(compose, "$prefix-${page++}")
        val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        if (range.value() >= range.maxValue()) break
        val step = scroll.fetchSemanticsNode().boundsInRoot.height * .65f
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, step) }
        check(page < 100) { "Scroll did not reach the end of $prefix" }
    } while (true)
}

fun captureFrame(compose: ComposeTestRule, name: String) {
    compose.waitForIdle()
    SystemClock.sleep(100)
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
