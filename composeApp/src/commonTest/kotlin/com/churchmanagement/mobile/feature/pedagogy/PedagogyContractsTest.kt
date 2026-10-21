package com.churchmanagement.mobile.feature.pedagogy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class PedagogyContractsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val summary = """{
        "filter":{"organization":"ong","period":"2026-2","cycle":1},
        "generatedAt":"2026-10-21T12:00:00Z","totalRecords":1,"detailsLoaded":false,
        "studentCount":1,"methodology":"N/O excluído",
        "records":[{"id":"a","studentId":"s","studentName":"Ana","classGroup":"Artes",
          "moment":"initial","evaluationDate":"2026-10-21","cycle":1,
          "answers":{"emotions":"NO"},"createdAt":"2026-10-21T12:00:00Z"}],
        "evolution":[{"moment":"initial","indices":{"idg":null,"ide":{"eu":null},"idd":{},
          "observedQuestions":0,"maximumPoints":0},"students":1,"evaluations":1}]
    }"""

    @Test
    fun summaryDoesNotRequireSensitiveDetailsOrConvertNullToZero() {
        val report = json.decodeFromString<EvaluationReport>(summary)
        assertFalse(report.detailsLoaded)
        assertTrue(report.records.single().observations.isEmpty())
        assertEquals("Não calculado", indexLabel(report.evolution.single().indices.idg))
        assertEquals("NO", report.records.single().answers["emotions"])
    }

    @Test
    fun exportRejectsIncompleteSummary() {
        val instrument = EvaluationInstrument(1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertFailsWith<IllegalArgumentException> { evaluationDocument(json.decodeFromString(summary), instrument) }
    }

    @Test
    fun retryKeepsIdempotencyKeyAndSuccessfulSaveStartsANewRequest() {
        val form = EvaluationFormState()
        form.answers = mapOf("emotions" to "A")
        val requestId = form.request("ong").requestId
        assertEquals(requestId, form.request("ong").requestId)
        form.saved()
        assertTrue(requestId != form.request("ong").requestId)
        assertTrue(form.answers.isEmpty())
    }

    @Test
    fun editingPayloadAfterFailureUsesANewIdempotencyKey() {
        val form = EvaluationFormState()
        val previous = form.request("ong").requestId
        form.student = "Ana"
        assertTrue(previous != form.request("ong").requestId)
    }
}
