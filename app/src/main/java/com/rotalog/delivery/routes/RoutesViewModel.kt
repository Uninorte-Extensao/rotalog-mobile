package com.rotalog.delivery.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotalog.delivery.data.DemoScenarioController
import com.rotalog.delivery.data.RouteRepository
import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoutesViewModel(
    private val repository: RouteRepository,
    private val demo: DemoScenarioController,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val selected = savedState.getStateFlow<String?>("stop", null)
    private val detailsOpen = savedState.getStateFlow("detailsOpen", savedState.get<Boolean>("expanded") ?: false)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    init {
        val restored = savedState.get<String>("scenario")?.let { value -> DemoScenario.entries.find { it.name == value } }
        if (restored != null && restored != demo.scenario.value) demo.selectScenario(restored)
        savedState.get<Route>("routeSnapshot")?.let(demo::restoreRoute)
        // An accepted demo is restorable, but accepting never receives cargo or starts the route.
        if (savedState.get<Boolean>("accepted") == true && demo.scenario.value == DemoScenario.ASSIGNED) {
            viewModelScope.launch { repository.route.value?.let { repository.acceptRoute(it.id) } }
        }
        savedState.get<String>("sync")?.let { value -> SyncStatus.entries.find { it.name == value }?.let(demo::simulateSync) }
        if (detailsOpen.value) {
            // The previous sheet could expand the next stop without persisting a marker selection.
            val route = repository.route.value
            val stopId = selected.value ?: route?.nextStop?.id
            if (stopId != null && route?.stops?.any { it.id == stopId } == true) savedState["stop"] = stopId
            else closeDetails()
        }
        viewModelScope.launch {
            var routeId = repository.route.value?.id
            repository.route.collect { route ->
                if (routeId != route?.id || (selected.value != null && route?.stops?.none { it.id == selected.value } != false)) {
                    closeDetails()
                }
                routeId = route?.id
            }
        }
    }

    private val content = combine(demo.scenario, repository.route, selected, detailsOpen, message) { scenario, route, id, open, notice ->
        val validSelection = route?.stops?.find { it.id == id }?.id
        RoutesUiState(
            scenario = scenario, route = route,
            selectedStopId = validSelection ?: route?.nextStop?.id,
            detailsOpen = open && validSelection != null, loading = false, message = notice
        )
    }
    val uiState: StateFlow<RoutesUiState> = combine(content, busy, error) { state, loading, failure ->
        state.copy(loading = loading, error = failure)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RoutesUiState())

    fun selectStop(id: String) {
        if (repository.route.value?.stops?.none { it.id == id } != false) return
        savedState["stop"] = id
    }
    fun openDetails(id: String) {
        if (repository.route.value?.stops?.none { it.id == id } != false) return
        savedState["stop"] = id
        savedState["detailsOpen"] = true
    }
    fun closeDetails() { savedState["detailsOpen"] = false; savedState["expanded"] = false }
    fun nextStop() {
        savedState["stop"] = repository.route.value?.nextStop?.id
        savedState["detailsOpen"] = false
    }
    fun selectScenario(scenario: DemoScenario) {
        if (scenario != demo.scenario.value) resetToScenario(scenario)
    }
    private fun resetToScenario(scenario: DemoScenario) {
        savedState["scenario"] = scenario.name
        savedState["stop"] = null
        savedState["detailsOpen"] = false
        savedState["accepted"] = false
        savedState["routeSnapshot"] = null
        savedState["sync"] = SyncStatus.LOCAL.name
        message.value = null
        error.value = null
        demo.selectScenario(scenario)
    }
    fun resetScenario() { resetToScenario(demo.scenario.value) }
    fun simulateSync(status: SyncStatus) {
        savedState["sync"] = status.name
        demo.simulateSync(status)
    }
    fun acceptRoute() {
        if (busy.value) return
        val id = repository.route.value?.id ?: return
        busy.value = true
        viewModelScope.launch {
            try {
                message.value = when (repository.acceptRoute(id)) {
                    AcceptRouteResult.Accepted -> {
                        savedState["accepted"] = true
                        savedState["routeSnapshot"] = repository.route.value
                        savedState["sync"] = repository.route.value?.syncStatus?.name
                        "Aceite salvo nesta demonstração. Preparação e recebimento continuam pendentes."
                    }
                    AcceptRouteResult.AlreadyAccepted -> "O aceite desta rota já foi confirmado."
                    AcceptRouteResult.NotAvailable -> "A rota não está disponível para aceite."
                }
            } catch (exception: java.io.IOException) {
                error.value = "Não foi possível aceitar a rota. Tente novamente."
            } finally { busy.value = false }
        }
    }
    private fun command(success: String? = null, action: suspend (String) -> RouteCommandResult) {
        if (busy.value) return
        val id = repository.route.value?.id ?: return
        error.value = null
        message.value = null
        busy.value = true
        viewModelScope.launch {
            try {
                when (val result = action(id)) {
                    is RouteCommandResult.Rejected -> error.value = result.reason
                    else -> {
                        savedState["routeSnapshot"] = repository.route.value
                        savedState["sync"] = repository.route.value?.syncStatus?.name
                        if (success != null) message.value = success
                    }
                }
            } catch (_: java.io.IOException) { error.value = "Não foi possível salvar. Tente novamente." }
            finally { busy.value = false }
        }
    }
    fun setPreparationCheck(check: PreparationCheck, checked: Boolean) = command { repository.setPreparationCheck(it, check, checked) }
    fun confirmPreparation() = command("Checklist confirmado nesta demonstração.", repository::confirmPreparation)
    fun checkCargo(productId: String, checked: Boolean) = command { repository.checkCargo(it, productId, checked) }
    fun reportCargoIssue(issue: CargoIssue) = command("Divergência salva localmente. Resolva com a equipe do CD antes de receber.") { repository.reportCargoIssue(it, issue) }
    fun resolveCargoIssue(productId: String) = command("Resolução registrada na demonstração. Confira novamente o produto.") { repository.resolveCargoIssue(it, productId) }
    fun confirmReceipt() = command("Recebimento confirmado nesta demonstração.", repository::confirmReceipt)
    fun startRoute() {
        command("Rota demonstrativa iniciada • sem GPS ou envio ao servidor.") {
            repository.startRoute(it).also { result -> if (result !is RouteCommandResult.Rejected) nextStop() }
        }
    }
    fun clearMessage() { message.value = null }
}
