package com.rotalog.delivery.history

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.ui.theme.RotalogTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryConsultationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unavailableRecordOffersReturn() {
        var returned = false
        compose.setContent { RotalogTheme { HistoryRouteScreen(null) { returned = true } } }
        compose.onNodeWithText("Rota do histórico indisponível").assertIsDisplayed()
        compose.onNodeWithText("Voltar ao Histórico").performClick()
        assertTrue(returned)
    }

    @Test fun expandsOnlyOneStopAtATimeAndKeepsOrder() {
        val route = DemoRoutes.history().single()
        compose.setContent { RotalogTheme { HistoryRouteScreen(route) {} } }
        val first = route.stops.first()
        val second = route.stops[1]
        compose.onNodeWithText("${first.sequence}. ${first.name}").performScrollTo().performClick()
        compose.onNodeWithText(first.address, substring = false).assertExists()
        compose.onNodeWithText("${second.sequence}. ${second.name}").performScrollTo().performClick()
        compose.onNodeWithText(first.instructions).assertDoesNotExist()
        compose.onNodeWithText(second.instructions).assertExists()
        compose.onNodeWithText("${second.sequence}. ${second.name}").performScrollTo().performClick()
        compose.onNodeWithText(second.instructions).assertDoesNotExist()
    }
}
