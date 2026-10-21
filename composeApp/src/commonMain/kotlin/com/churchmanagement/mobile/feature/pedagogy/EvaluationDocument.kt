package com.churchmanagement.mobile.feature.pedagogy

data class EvaluationDocument(val filename: String, val lines: List<String>)

fun evaluationDocument(report: EvaluationReport, instrument: EvaluationInstrument): EvaluationDocument {
    require(report.detailsLoaded) { "Carregue o relatório completo antes de exportar." }
    val lines = buildList {
        add("Relatório qualitativo individual e coletivo")
        add("${if (report.filter.organization == "ong") "ONG" else "Igreja"} · ${report.filter.period}")
        add(instrument.cycles.first { it.id == report.filter.cycle }.label)
        add("Turma: ${report.filter.classGroup ?: "Todas as turmas"}")
        add("Assistido: ${report.filter.studentId?.let { id -> report.records.firstOrNull { it.studentId == id }?.studentName } ?: "Coletivo"}")
        add("Gerado em: ${report.generatedAt}")
        add("${report.studentCount} assistido(s) · ${report.totalRecords} avaliação(ões)")
        report.evolution.forEach { moment ->
            add(instrument.moments.labelFor(moment.moment))
            add("IDG: ${indexLabel(moment.indices.idg)}")
            moment.indices.ide.forEach { (axis, value) -> add("IDE ${axis.uppercase()}: ${indexLabel(value)}") }
            instrument.dimensions.forEach { add("IDD ${it.label}: ${indexLabel(moment.indices.idd[it.id])}") }
        }
        add(report.methodology)
        add("Avaliações e acompanhamento qualitativo")
        report.records.forEach { addAll(evaluationRecordLines(it, instrument)) }
    }
    return EvaluationDocument("avaliacao-${report.filter.organization}-${report.filter.period}-ciclo-${report.filter.cycle}", lines)
}

fun evaluationRecordLines(record: EvaluationRecord, instrument: EvaluationInstrument): List<String> = buildList {
    add("${record.studentName} · ${record.classGroup} · ${record.evaluationDate}")
    add("${record.educatorName} · ${record.age} anos · ${instrument.moments.labelFor(record.moment)}")
    add("IDG: ${indexLabel(record.indices.idg)}")
    instrument.cycles.first { it.id == record.cycle }.questions.forEach { question ->
        add("${question.text}: ${instrument.answers.labelFor(record.answers[question.dimensionId].orEmpty())}")
    }
    instrument.observations.forEach { add("${it.label}: ${record.observations[it.id].orEmpty().ifBlank { "Sem observação registrada" }}") }
    add("Encaminhamentos: ${record.referrals.joinToString { instrument.referrals.labelFor(it) }}")
    if (record.otherReferral.isNotBlank()) add("Outro encaminhamento: ${record.otherReferral}")
}
