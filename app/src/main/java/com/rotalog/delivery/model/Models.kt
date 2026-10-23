package com.rotalog.delivery.model

enum class DemoScenario(val label: String) {
    ASSIGNED("Rota atribuída"), EMPTY("Sem rota atribuída"), ACTIVE("Rota ativa demonstrativa")
}
enum class ThemeMode(val label: String) { SYSTEM("Usar tema do aparelho"), LIGHT("Claro"), DARK("Escuro") }
enum class SyncStatus(val label: String) {
    LOCAL("Salvo no aparelho"), WAITING("Aguardando envio"), SENDING("Enviando"),
    CONFIRMED("Confirmado"), FAILED("Falha no envio"), CONFLICT("Conflito")
}
enum class RouteStatus(val label: String) { ASSIGNED("Aguardando aceite"), ACCEPTED("Aceite confirmado"), ACTIVE("Em andamento"), COMPLETED("Concluída") }
enum class StopType(val label: String) { COLLECTION("Coleta"), DELIVERY("Entrega"), RETURN("Retorno à base") }
enum class StopStatus(val label: String) { PENDING("Pendente"), COMPLETED("Concluída") }
data class StopItem(val productId: String, val description: String, val unit: String, val quantity: Int) : java.io.Serializable
data class Stop(
    val id: String, val sequence: Int, val type: StopType, val status: StopStatus,
    val name: String, val address: String, val reference: String, val window: String,
    val contact: String, val phone: String, val instructions: String, val items: List<StopItem>,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
) : java.io.Serializable { val volumes: Int get() = items.sumOf { it.quantity } }
data class Route(
    val id: String, val code: String, val date: String, val status: RouteStatus,
    val cargoReceived: Boolean, val prepared: Boolean, val stops: List<Stop>,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val checklist: Set<PreparationCheck> = emptySet(),
    val checkedCargo: Set<String> = emptySet(),
    val cargoIssues: List<CargoIssue> = emptyList()
) : java.io.Serializable {
    val manifest: List<StopItem> get() = stops.filter { it.type == StopType.COLLECTION }.flatMap { it.items }
    val openCargoIssues: List<CargoIssue> get() = cargoIssues.filterNot { it.resolved }
    val canReceive: Boolean get() = status == RouteStatus.ACCEPTED && prepared && manifest.isNotEmpty() &&
        manifest.all { it.productId in checkedCargo } && openCargoIssues.isEmpty()
    val canStart: Boolean get() = status == RouteStatus.ACCEPTED && prepared && cargoReceived && openCargoIssues.isEmpty()
    val completedStops: Int get() = stops.count { it.status == StopStatus.COMPLETED }
    val nextStop: Stop? get() = stops.sortedBy { it.sequence }.firstOrNull { it.status == StopStatus.PENDING }
    val deliveryVolumes: Int get() = stops.filter { it.type == StopType.DELIVERY }.sumOf { it.volumes }
    val collectionVolumes: Int get() = stops.filter { it.type == StopType.COLLECTION }.sumOf { it.volumes }
    val deliveredVolumes: Int get() = stops.filter { it.type == StopType.DELIVERY && it.status == StopStatus.COMPLETED }.sumOf { it.volumes }
    val collectedVolumes: Int get() = stops.filter { it.type == StopType.COLLECTION && it.status == StopStatus.COMPLETED }.sumOf { it.volumes }
    val progress: Float get() = if (stops.isEmpty()) 0f else completedStops.toFloat() / stops.size
}
sealed interface AcceptRouteResult {
    data object Accepted : AcceptRouteResult
    data object AlreadyAccepted : AcceptRouteResult
    data object NotAvailable : AcceptRouteResult
}

enum class PreparationCheck(val label: String) {
    FUEL("Combustível suficiente verificado"), TIRES("Pneus calibrados e em bom estado"),
    BATTERY("Bateria do celular carregada"), CARGO_AREA("Área de carga livre e limpa")
}
enum class CargoIssueReason(val label: String) {
    MISSING("Volume faltante"), DAMAGED("Embalagem avariada"), WRONG_PRODUCT("Produto ou lote divergente"), LEAK("Vazamento")
}
data class CargoIssue(val productId: String, val reason: CargoIssueReason, val note: String, val resolved: Boolean = false) : java.io.Serializable
sealed interface RouteCommandResult {
    data object Applied : RouteCommandResult
    data object AlreadyApplied : RouteCommandResult
    data class Rejected(val reason: String) : RouteCommandResult
}
