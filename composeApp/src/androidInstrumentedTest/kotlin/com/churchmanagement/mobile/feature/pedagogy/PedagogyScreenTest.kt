package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.churchmanagement.mobile.platform.ReportExporter
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class PedagogyScreenTest {
    @get:Rule val compose = createComposeRule()
    private val gateway = TestPedagogyGateway()
    private val exports = CopyOnWriteArrayList<EvaluationDocument>()

    @Before
    fun dependencies() {
        loadKoinModules(module {
            single<PedagogyGateway> { gateway }
            single<ReportExporter> {
                object : ReportExporter {
                    override val formats = listOf("PDF", "Word")
                    override suspend fun export(document: EvaluationDocument, format: String) { exports.add(document) }
                }
            }
        })
    }

    @Test
    fun applicationsEvaluationAndReportsHaveDistinctContent() {
        showScreen()
        compose.onNodeWithText("Relatório individual e coletivo").assertIsDisplayed()
        assertTrue(gateway.filters.none { it.includeDetails })
        compose.onNodeWithText("Aplicação").performClick()
        compose.onNodeWithText("Aplicação das diretrizes").assertIsDisplayed()
        compose.onNodeWithText("Relatório individual e coletivo").assertDoesNotExist()
        compose.onNodeWithText("Avaliação").performClick()
        compose.onNodeWithText("Avaliação do desenvolvimento").assertIsDisplayed()
        compose.onNodeWithText("Aplicação das diretrizes").assertDoesNotExist()
    }

    @Test
    fun readOnlyUserCannotRecordAndExportLoadsFullDetailsOnDemand() {
        gateway.access = PedagogyAccess(view = true, create = false, collective = true)
        showScreen()
        compose.onNodeWithText("Avaliação").assertDoesNotExist()
        compose.onNodeWithText("Exportar PDF").performClick()
        compose.waitUntil(5_000) { exports.isNotEmpty() }
        assertEquals(listOf(false, true), gateway.filters.map { it.includeDetails })
        assertTrue(exports.single().lines.any { it.contains("Potencial: Criatividade") })
    }

    private fun showScreen() {
        compose.setContent { MaterialTheme { Box(Modifier.fillMaxSize()) { PedagogyScreen() } } }
        compose.waitUntil(5_000) { gateway.filters.isNotEmpty() }
        compose.waitForIdle()
    }
}

private class TestPedagogyGateway : PedagogyGateway {
    var access = PedagogyAccess(view = true, create = true, collective = true)
    val filters = CopyOnWriteArrayList<EvaluationFilter>()
    private val instrument = EvaluationInstrument(
        version = 1,
        dimensions = listOf(EvaluationDimension("emotions", "eu", "Emoções")),
        cycles = listOf(EvaluationCycle(1, "Ciclo de teste", listOf(EvaluationQuestion("emotions", "Como se expressa?", "Exemplo")))),
        moments = listOf(InstrumentOption("initial", "Inicial")),
        answers = listOf(InstrumentOption("A", "Sempre")),
        referrals = listOf(InstrumentOption("none", "Sem necessidade")),
        observations = listOf(InstrumentOption("potential", "Potencial")),
    )

    override suspend fun workspace(request: WorkspaceRequest) = PedagogyWorkspace(access, instrument, emptyList())

    override suspend fun report(filter: EvaluationFilter): EvaluationReport {
        filters.add(filter)
        val record = EvaluationRecord(
            id = "record", studentId = "student", studentName = "Ana", classGroup = "Artes",
            moment = "initial", evaluationDate = "2026-10-21", cycle = 1,
            answers = mapOf("emotions" to "A"), observations = if (filter.includeDetails) mapOf("potential" to "Criatividade") else emptyMap(),
        )
        return EvaluationReport(filter, "2026-10-21", listOf(record), 1, filter.includeDetails, emptyList(), 1, "N/O excluído")
    }

    override suspend fun create(request: EvaluationRequest): EvaluationRecord = error("Não solicitado neste teste")
}
