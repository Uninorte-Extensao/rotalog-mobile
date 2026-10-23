package com.rotalog.delivery.data

import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fixtures. Demo commands never represent server acknowledgements. */
class DemoRouteRepository : RouteRepository, DemoScenarioController {
    private val currentScenario = MutableStateFlow(DemoScenario.ASSIGNED)
    override val scenario = currentScenario.asStateFlow()
    private val currentRoute = MutableStateFlow<Route?>(DemoRoutes.assigned())
    override val route = currentRoute.asStateFlow()
    override val history = MutableStateFlow(DemoRoutes.history()).asStateFlow()

    override suspend fun acceptRoute(routeId: String): AcceptRouteResult {
        val route = currentRoute.value ?: return AcceptRouteResult.NotAvailable
        if (route.id != routeId) return AcceptRouteResult.NotAvailable
        if (route.status != RouteStatus.ASSIGNED) return AcceptRouteResult.AlreadyAccepted
        currentRoute.value = route.copy(status = RouteStatus.ACCEPTED, syncStatus = SyncStatus.LOCAL)
        return AcceptRouteResult.Accepted
    }

    private fun updateAssigned(routeId: String, change: (Route) -> RouteCommandResult): RouteCommandResult {
        val route = currentRoute.value
        if (route == null || route.id != routeId) return RouteCommandResult.Rejected("Esta rota não está disponível.")
        if (route.status != RouteStatus.ACCEPTED) return RouteCommandResult.Rejected("Aceite a rota antes de preparar a saída.")
        return change(route)
    }
    private fun save(route: Route): RouteCommandResult {
        currentRoute.value = route.copy(syncStatus = SyncStatus.LOCAL)
        return RouteCommandResult.Applied
    }
    override suspend fun setPreparationCheck(routeId: String, check: PreparationCheck, checked: Boolean) = updateAssigned(routeId) { route ->
        val checks = if (checked) route.checklist + check else route.checklist - check
        save(route.copy(checklist = checks, prepared = route.prepared && checks.containsAll(PreparationCheck.entries)))
    }
    override suspend fun confirmPreparation(routeId: String) = updateAssigned(routeId) { route ->
        when {
            route.prepared -> RouteCommandResult.AlreadyApplied
            !route.checklist.containsAll(PreparationCheck.entries) -> RouteCommandResult.Rejected("Verifique os quatro itens do checklist.")
            else -> save(route.copy(prepared = true))
        }
    }
    override suspend fun checkCargo(routeId: String, productId: String, checked: Boolean) = updateAssigned(routeId) { route ->
        when {
            route.cargoReceived -> RouteCommandResult.Rejected("O recebimento já foi confirmado; a conferência está disponível para consulta.")
            !route.prepared -> RouteCommandResult.Rejected("Conclua o checklist antes de conferir a carga.")
            route.manifest.none { it.productId == productId } -> RouteCommandResult.Rejected("Produto fora do manifesto.")
            route.openCargoIssues.any { it.productId == productId } && checked -> RouteCommandResult.Rejected("Resolva a divergência deste produto antes de conferir.")
            else -> save(route.copy(checkedCargo = if (checked) route.checkedCargo + productId else route.checkedCargo - productId))
        }
    }
    override suspend fun reportCargoIssue(routeId: String, issue: CargoIssue) = updateAssigned(routeId) { route ->
        when {
            !route.prepared || route.cargoReceived -> RouteCommandResult.Rejected("Registre divergências durante a conferência, antes do recebimento.")
            route.manifest.none { it.productId == issue.productId } -> RouteCommandResult.Rejected("Produto fora do manifesto.")
            issue.note.length > 500 -> RouteCommandResult.Rejected("Use até 500 caracteres nas observações.")
            else -> save(route.copy(cargoIssues = route.cargoIssues.filterNot { it.productId == issue.productId } + issue.copy(note = issue.note.trim(), resolved = false), checkedCargo = route.checkedCargo - issue.productId))
        }
    }
    override suspend fun resolveCargoIssue(routeId: String, productId: String) = updateAssigned(routeId) { route ->
        when {
            route.cargoReceived || route.openCargoIssues.none { it.productId == productId } -> RouteCommandResult.Rejected("Não há divergência aberta para este produto.")
            else -> save(route.copy(cargoIssues = route.cargoIssues.map { if (it.productId == productId) it.copy(resolved = true) else it }))
        }
    }
    override suspend fun confirmReceipt(routeId: String) = updateAssigned(routeId) { route ->
        when {
            route.cargoReceived -> RouteCommandResult.AlreadyApplied
            !route.canReceive -> RouteCommandResult.Rejected("Conclua o checklist, confira todos os produtos e resolva as divergências.")
            else -> save(route.copy(cargoReceived = true, stops = route.stops.map {
                if (it.type == StopType.COLLECTION) it.copy(status = StopStatus.COMPLETED) else it
            }))
        }
    }
    override suspend fun startRoute(routeId: String): RouteCommandResult {
        val route = currentRoute.value
        if (route?.id == routeId && route.status == RouteStatus.ACTIVE) return RouteCommandResult.AlreadyApplied
        return updateAssigned(routeId) {
            if (!it.canStart) RouteCommandResult.Rejected("Confirme o preparo e o recebimento antes de iniciar.")
            else save(it.copy(status = RouteStatus.ACTIVE))
        }
    }

    override fun selectScenario(scenario: DemoScenario) {
        currentScenario.value = scenario
        resetScenario()
    }

    override fun restoreRoute(snapshot: Route) {
        if (snapshot.id == currentRoute.value?.id) currentRoute.value = snapshot
    }

    override fun resetScenario() {
        currentRoute.value = when (scenario.value) {
            DemoScenario.ASSIGNED -> DemoRoutes.assigned()
            DemoScenario.EMPTY -> null
            DemoScenario.ACTIVE -> DemoRoutes.active()
        }
    }

    override fun simulateSync(status: SyncStatus) {
        currentRoute.value = currentRoute.value?.copy(syncStatus = status)
    }
}

object DemoRoutes {
    private fun items(id: String, quantity: Int) = listOf(StopItem(id, when (id) {
        "higiene" -> "Produtos de higiene"
        "limpeza" -> "Produtos de limpeza"
        "mercearia" -> "Mercearia seca"
        "alimentos" -> "Alimentos embalados"
        "bebidas" -> "Bebidas"
        else -> "Carga da rota"
    }, "caixas", quantity))
    private fun stops(): List<Stop> {
        val itinerary = listOf(
            Stop("cd", 1, StopType.COLLECTION, StopStatus.PENDING, "CD Central Manaus",
                "Av. Rodrigo Otávio, 1000 - Distrito Industrial I, Manaus - AM", "Doca de expedição 2", "13:00 – 13:20",
                "Mariana • expedição", "+5592900000000", "Aguardar a equipe na doca de expedição. Conferir os volumes antes da saída.", items("carga-rota", 70),
                latitude = -3.1000, longitude = -59.9700),
            Stop("rio-negro", 2, StopType.DELIVERY, StopStatus.PENDING, "Drogaria Rio Negro",
                "Av. Sete de Setembro, 800 - Centro, Manaus - AM", "Entrada de serviço lateral", "13:30 – 14:00",
                "Ana • responsável", "+5592900000000", "Solicitar o responsável pelo recebimento na entrada lateral.", items("higiene", 8),
                latitude = -3.1330, longitude = -60.0230),
            Stop("amazonas", 3, StopType.DELIVERY, StopStatus.PENDING, "Supermercado Amazonas",
                "Av. Djalma Batista, 1420 - Chapada, Manaus - AM", "Portão B, pela Rua Rio Purus", "14:00 – 15:30",
                "Roberto (gerente)", "+5592900000000", "Acesso pelos fundos, Portão B (Rua Rio Purus). Apresentar nota fiscal e aguardar conferência na recepção de mercadorias.",
                listOf(StopItem("oleo", "Óleo", "caixas", 8), StopItem("arroz", "Arroz", "fardos", 4), StopItem("cafe", "Café", "caixas", 2)),
                latitude = -3.0950, longitude = -60.0200),
            Stop("ponta-negra", 4, StopType.DELIVERY, StopStatus.PENDING, "Distribuidora Ponta Negra",
                "Av. Coronel Teixeira, 520 - Ponta Negra, Manaus - AM", "Área de descarga sinalizada", "15:45 – 16:30",
                "João • recebimento", "+5592900000000", "Estacionar na área de descarga e aguardar a conferência.", items("limpeza", 20),
                latitude = -3.0780, longitude = -60.0900),
            Stop("adrianopolis", 5, StopType.DELIVERY, StopStatus.PENDING, "Empório Adrianópolis",
                "R. Teresina, 310 - Adrianópolis, Manaus - AM", "Porta lateral do empório", "16:45 – 17:15",
                "Luiza • responsável", "+5592900000000", "Entregar pela porta lateral. Manter as caixas protegidas da chuva.", items("mercearia", 6),
                latitude = -3.1090, longitude = -60.0210),
            Stop("parque-dez", 6, StopType.DELIVERY, StopStatus.PENDING, "Mercantil Parque Dez",
                "Av. Nilton Lins, 880 - Parque Dez, Manaus - AM", "Ao lado do estacionamento", "17:30 – 18:00",
                "Paulo • recebimento", "+5592900000000", "Usar a entrada de fornecedores ao lado do estacionamento.", items("alimentos", 12),
                latitude = -3.0990, longitude = -60.0000),
            Stop("cidade-nova", 7, StopType.DELIVERY, StopStatus.PENDING, "Varejão Cidade Nova",
                "Av. Max Teixeira, 190 - Cidade Nova, Manaus - AM", "Portão de recebimento", "18:15 – 18:45",
                "Rita • responsável", "+5592900000000", "Aguardar a liberação do portão antes de descarregar.", items("bebidas", 10),
                latitude = -3.0330, longitude = -59.9780),
            Stop("educandos", 8, StopType.DELIVERY, StopStatus.PENDING, "Atacadão Educandos",
                "Av. Leopoldo Peres, 600 - Educandos, Manaus - AM", "Doca de recebimento", "18:50 – 19:20",
                "Marcos • recebimento", "+5592900000000", "Apresentar a nota fiscal na doca de recebimento e aguardar a conferência.", items("mercearia", 7),
                latitude = -3.1400, longitude = -60.0000),
            Stop("taruma", 9, StopType.RETURN, StopStatus.PENDING, "Retorno Garagem Tarumã",
                "Av. do Turismo, 400 - Tarumã, Manaus - AM", "Portaria da garagem", "19:45",
                "Equipe da base", "+5592900000000", "Apresentar-se à equipe da base para os procedimentos de retorno.", emptyList(),
                latitude = -3.0500, longitude = -60.0700)
        )


        return itinerary.map { stop ->
            if (stop.type == StopType.COLLECTION) stop.copy(items = itinerary.filter { it.type == StopType.DELIVERY }.flatMap { it.items }) else stop
        }
    }

    fun assigned() = Route("assigned-mao-042", "ROTA-MAO-042", "22/10/2026", RouteStatus.ASSIGNED, false, false, stops())
    private fun withConfirmedLoading(route: Route) = route.copy(
        checklist = PreparationCheck.entries.toSet(), checkedCargo = route.manifest.map { it.productId }.toSet()
    )
    fun active() = withConfirmedLoading(Route("demo-active-mao-042", "ROTA-MAO-042", "22/10/2026", RouteStatus.ACTIVE, true, true,
        stops().map { if (it.sequence <= 2) it.copy(status = StopStatus.COMPLETED) else it }))

    fun history(): List<Route> = listOf(
        withConfirmedLoading(Route("history-mao-041", "ROTA-MAO-041", "21/10/2026", RouteStatus.COMPLETED, true, true,
            stops().map { it.copy(id = "previous-${it.id}", status = StopStatus.COMPLETED) }, SyncStatus.CONFIRMED))
    )
}
