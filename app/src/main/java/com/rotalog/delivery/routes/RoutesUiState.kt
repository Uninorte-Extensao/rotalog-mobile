package com.rotalog.delivery.routes

import com.rotalog.delivery.model.*

data class RoutesUiState(
    val scenario: DemoScenario = DemoScenario.ASSIGNED,
    val route: Route? = null,
    val selectedStopId: String? = null,
    val detailsOpen: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null
) {
    val selectedStop: Stop? get() = route?.stops?.find { it.id == selectedStopId } ?: route?.nextStop
}
