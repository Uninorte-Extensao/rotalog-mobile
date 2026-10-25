package com.rotalog.delivery.loading

import androidx.lifecycle.SavedStateHandle
import com.rotalog.delivery.data.*
import com.rotalog.delivery.model.*
import com.rotalog.delivery.routes.RoutesViewModel
import java.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingRulesTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun globalConfirmationIsAtomicAndRequiresAcceptance() = runTest {
        val repo = DemoRouteRepository(); val id = repo.route.value!!.id
        assertTrue(repo.setCargoConfirmation(id, true) is RouteCommandResult.Rejected)
        repo.acceptRoute(id)
        assertEquals(RouteCommandResult.Applied, repo.setCargoConfirmation(id, true))
        assertEquals(repo.route.value!!.manifest.map { it.productId }.toSet(), repo.route.value!!.checkedCargo)
        assertFalse(repo.route.value!!.cargoReceived)
        assertEquals(RouteCommandResult.Applied, repo.setCargoConfirmation(id, false))
        assertTrue(repo.route.value!!.checkedCargo.isEmpty())
    }
    private suspend fun accepted(repo: DemoRouteRepository): String {
        val id = repo.route.value!!.id
        repo.acceptRoute(id)
        return id
    }
    @Test fun cargoAndDepartureDoNotRequireLegacyPreparation() = runTest {
        val repo = DemoRouteRepository(); val id = repo.route.value!!.id
        repo.acceptRoute(id)
        assertEquals(RouteCommandResult.Applied, repo.checkCargo(id, "oleo", true))
        val confirmed = repo.route.value!!.copy(checkedCargo = repo.route.value!!.manifest.map { it.productId }.toSet())
        repo.restoreRoute(confirmed)
        assertTrue(repo.route.value!!.canReceive)
        assertEquals(RouteCommandResult.Applied, repo.confirmReceipt(id))
        assertTrue(repo.route.value!!.canStart)
        assertEquals(RouteCommandResult.Applied, repo.startRoute(id))
        assertFalse(repo.route.value!!.prepared)
    }
    @Test fun explicitReceiptCompletesCollectionAndNeverStartsDelivery() = runTest {
        val repo = DemoRouteRepository(); val id = accepted(repo)
        assertEquals(77, repo.route.value!!.manifest.sumOf { it.quantity })
        assertEquals(9, repo.route.value!!.manifest.size)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.setCargoConfirmation(id, true)
        assertFalse(repo.route.value!!.cargoReceived)
        assertEquals(RouteCommandResult.Applied, repo.confirmReceipt(id))
        assertEquals(RouteCommandResult.AlreadyApplied, repo.confirmReceipt(id))
        val route = repo.route.value!!
        assertTrue(route.cargoReceived)
        assertEquals(RouteStatus.ACCEPTED, route.status)
        assertEquals(77, route.collectedVolumes)
        assertEquals(0, route.deliveredVolumes)
        assertEquals(1, route.completedStops)
        assertEquals("rio-negro", route.nextStop!!.id)
    }
    @Test fun unresolvedIssueInvalidatesCheckAndResolutionRequiresRecheck() = runTest {
        val repo = DemoRouteRepository(); val id = accepted(repo)
        repo.setCargoConfirmation(id, true)
        val issue = CargoIssue("oleo", CargoIssueReason.DAMAGED, "Caixa amassada")
        repo.reportCargoIssue(id, issue)
        assertFalse("oleo" in repo.route.value!!.checkedCargo)
        assertTrue(repo.setCargoConfirmation(id, true) is RouteCommandResult.Rejected)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.resolveCargoIssue(id, "oleo")
        assertTrue(repo.route.value!!.openCargoIssues.isEmpty())
        assertFalse("oleo" in repo.route.value!!.checkedCargo)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.setCargoConfirmation(id, true)
        repo.confirmReceipt(id)
        assertTrue(repo.reportCargoIssue(id, issue) is RouteCommandResult.Rejected)
        assertTrue(repo.setCargoConfirmation(id, false) is RouteCommandResult.Rejected)
        assertTrue(repo.route.value!!.cargoReceived)
    }
    @Test fun receiptAndDepartureIgnoreLegacyChecklistChanges() = runTest {
        val repo = DemoRouteRepository(); val id = repo.route.value!!.id
        repo.acceptRoute(id); repo.setCargoConfirmation(id, true); repo.confirmReceipt(id)
        assertFalse(repo.route.value!!.prepared)
        assertEquals(RouteCommandResult.Applied, repo.startRoute(id))
        assertEquals(RouteCommandResult.AlreadyApplied, repo.startRoute(id))
        assertEquals(0, repo.route.value!!.deliveredVolumes)
        assertEquals("rio-negro", repo.route.value!!.nextStop!!.id)
    }
    @Test fun snapshotSurvivesSerializationAndRestoresPartialLoadingWithoutAutoConfirmation() = runTest {
        val repo = DemoRouteRepository(); val saved = SavedStateHandle()
        val vm = RoutesViewModel(repo, repo, saved)
        vm.acceptRoute()
        vm.setCargoConfirmation(true)
        vm.reportCargoIssue(CargoIssue("arroz", CargoIssueReason.MISSING, "Falta um fardo"))
        val snapshot = saved.get<Route>("routeSnapshot")!!
        val bytes = ByteArrayOutputStream().also { output -> ObjectOutputStream(output).use { it.writeObject(snapshot) } }.toByteArray()
        val restoredSnapshot = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Route }
        val fresh = DemoRouteRepository()
        val restored = RoutesViewModel(fresh, fresh, SavedStateHandle(mapOf("routeSnapshot" to restoredSnapshot)))
        assertEquals(snapshot, restored.uiState.value.route)
        assertFalse(restored.uiState.value.route!!.cargoReceived)
        assertEquals(1, restored.uiState.value.route!!.openCargoIssues.size)
        restored.selectScenario(DemoScenario.ACTIVE)
        restored.selectScenario(DemoScenario.ASSIGNED)
        assertEquals(DemoRoutes.assigned(), fresh.route.value)
    }

}
