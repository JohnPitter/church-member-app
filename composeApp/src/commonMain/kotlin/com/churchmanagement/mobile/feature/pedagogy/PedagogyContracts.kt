package com.churchmanagement.mobile.feature.pedagogy

import kotlinx.serialization.Serializable

@Serializable
data class InstrumentOption(val id: String, val label: String, val example: String = "")

@Serializable
data class EvaluationQuestion(val dimensionId: String, val text: String, val example: String)

@Serializable
data class EvaluationDimension(val id: String, val axis: String, val label: String)

@Serializable
data class EvaluationCycle(val id: Int, val label: String, val questions: List<EvaluationQuestion>)

@Serializable
data class EvaluationInstrument(
    val version: Int,
    val dimensions: List<EvaluationDimension>,
    val cycles: List<EvaluationCycle>,
    val moments: List<InstrumentOption>,
    val answers: List<InstrumentOption>,
    val referrals: List<InstrumentOption>,
    val observations: List<InstrumentOption>,
)

@Serializable
data class PedagogyAccess(val view: Boolean, val create: Boolean, val collective: Boolean)

@Serializable
data class AttendanceStudent(val name: String)

@Serializable
data class WorkspaceRecord(
    val id: String,
    val classGroup: String = "",
    val sessionDate: String = "",
    val students: List<AttendanceStudent> = emptyList(),
    val guidelineTitle: String = "",
    val educatorName: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val applicationNarrative: String = "",
    val applicationDifficulties: String = "",
    val strategies: String = "",
    val observedResults: String = "",
)

@Serializable
data class PedagogyWorkspace(
    val access: PedagogyAccess,
    val instrument: EvaluationInstrument,
    val records: List<WorkspaceRecord>,
)

@Serializable
data class WorkspaceRequest(val organization: String, val section: String)

@Serializable
data class EvaluationFilter(
    val organization: String,
    val period: String,
    val cycle: Int = 1,
    val classGroup: String? = null,
    val studentId: String? = null,
    val includeDetails: Boolean = false,
)

@Serializable
data class EvaluationRequest(
    val requestId: String,
    val organization: String,
    val attendanceRollId: String,
    val studentName: String,
    val age: Int,
    val cycle: Int,
    val moment: String,
    val period: String,
    val evaluationDate: String,
    val answers: Map<String, String>,
    val observations: Map<String, String>,
    val referrals: List<String>,
    val otherReferral: String,
)

@Serializable
data class DevelopmentIndices(
    val idg: Double? = null,
    val ide: Map<String, Double?> = emptyMap(),
    val idd: Map<String, Double?> = emptyMap(),
)

@Serializable
data class EvaluationRecord(
    val id: String,
    val studentId: String,
    val studentName: String,
    val classGroup: String,
    val moment: String,
    val evaluationDate: String,
    val educatorName: String = "",
    val age: Int = 0,
    val cycle: Int,
    val answers: Map<String, String> = emptyMap(),
    val observations: Map<String, String> = emptyMap(),
    val referrals: List<String> = emptyList(),
    val otherReferral: String = "",
    val indices: DevelopmentIndices = DevelopmentIndices(),
)

@Serializable
data class EvaluationEvolution(
    val moment: String,
    val indices: DevelopmentIndices,
    val students: Int,
    val evaluations: Int,
)

@Serializable
data class EvaluationReport(
    val filter: EvaluationFilter,
    val generatedAt: String,
    val records: List<EvaluationRecord>,
    val totalRecords: Int,
    val detailsLoaded: Boolean,
    val evolution: List<EvaluationEvolution>,
    val studentCount: Int,
    val methodology: String,
)

fun indexLabel(value: Double?): String = value?.let { "${it}%" } ?: "Não calculado"

fun List<InstrumentOption>.labelFor(id: String): String = firstOrNull { it.id == id }?.label ?: id
