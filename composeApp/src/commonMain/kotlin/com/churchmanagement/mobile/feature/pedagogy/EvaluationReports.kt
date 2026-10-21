package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.platform.ReportExporter
import com.churchmanagement.mobile.util.currentLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun EvaluationReports(organization: String, instrument: EvaluationInstrument) {
    val today = remember { currentLocalDate() }
    var filter by remember(organization) {
        mutableStateOf(EvaluationFilter(organization, "${today.year}-${if (today.monthNumber <= 6) 1 else 2}"))
    }
    var options by remember(organization, filter.period, filter.cycle) { mutableStateOf<List<EvaluationRecord>>(emptyList()) }
    var showFilters by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Relatório individual e coletivo", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = { showFilters = !showFilters }) {
            Text(if (showFilters) "Recolher filtros" else "Filtrar · ${filter.period} · Ciclo ${filter.cycle}")
        }
        if (showFilters) ReportFilters(filter, options, instrument) { filter = it }
        key(filter) {
            ReportContent(filter, instrument) { report ->
                if (filter.classGroup == null && filter.studentId == null) options = report.records
            }
        }
    }
}

@Composable
private fun ReportFilters(filter: EvaluationFilter, options: List<EvaluationRecord>, instrument: EvaluationInstrument, onChange: (EvaluationFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(filter.period, { onChange(filter.copy(period = it, classGroup = null, studentId = null)) },
            label = { Text("Período (ano-semestre, ex.: 2026-2)") }, modifier = Modifier.fillMaxWidth())
        PedagogySelect("Ciclo", filter.cycle.toString(), instrument.cycles.map { it.id.toString() to it.label }) {
            onChange(filter.copy(cycle = it.toInt(), classGroup = null, studentId = null))
        }
        PedagogySelect("Turma", filter.classGroup.orEmpty(), listOf("" to "Todas as turmas") + options.map { it.classGroup }.distinct().sorted().map { it to it }) {
            onChange(filter.copy(classGroup = it.ifBlank { null }, studentId = null))
        }
        val students = options.filter { filter.classGroup == null || it.classGroup == filter.classGroup }.distinctBy { it.studentId }
        PedagogySelect("Assistido", filter.studentId.orEmpty(), listOf("" to "Coletivo") + students.map { it.studentId to "${it.studentName} · ${it.classGroup}" }) {
            onChange(filter.copy(studentId = it.ifBlank { null }))
        }
    }
}

@Composable
private fun ReportContent(filter: EvaluationFilter, instrument: EvaluationInstrument, onLoaded: (EvaluationReport) -> Unit) {
    val repository: PedagogyGateway = koinInject()
    val exporter: ReportExporter = koinInject()
    val scope = rememberCoroutineScope()
    val state = remember { EvaluationReportState(repository, filter) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(retry) {
        delay(350)
        state.loadSummary(onLoaded)
    }
    val load: (String?) -> Unit = { format ->
        scope.launch {
            state.loadDetails { complete ->
                if (format != null) exporter.export(evaluationDocument(complete, instrument), format)
            }
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { item { PedagogyError(it) { if (state.report == null) retry++ else load(null) } } }
        val summary = state.report
        if (summary == null && state.error == null) item { CircularProgressIndicator() }
        if (summary != null) {
            item { Text("${summary.studentCount} assistido(s) · ${summary.totalRecords} avaliação(ões)") }
            if (summary.totalRecords == 0) item { Text("Nenhuma avaliação neste período e ciclo.") }
            if (summary.totalRecords > 0) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    exporter.formats.forEach { format ->
                        OutlinedButton(enabled = !state.busy, onClick = { load(format) }) { Text("Exportar $format") }
                    }
                }
            }
            items(summary.evolution, key = { it.moment }) { evolution -> EvolutionCard(evolution, instrument) }
            item { Text(summary.methodology, style = MaterialTheme.typography.bodySmall) }
            if (summary.totalRecords > 0 && state.details == null) item {
                Button(enabled = !state.busy, onClick = { load(null) }) { Text(if (state.busy) "Carregando..." else "Carregar histórico detalhado") }
            }
            items(state.details?.records.orEmpty(), key = { it.id }) { record -> EvaluationHistoryCard(record, instrument) }
        }
    }
}

@Composable
private fun EvolutionCard(evolution: EvaluationEvolution, instrument: EvaluationInstrument) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(instrument.moments.labelFor(evolution.moment), style = MaterialTheme.typography.titleMedium)
            Text("${evolution.students} assistido(s) · ${evolution.evaluations} avaliação(ões)")
            IndexBar("IDG", evolution.indices.idg)
            evolution.indices.ide.forEach { (axis, value) -> IndexBar("IDE ${axis.uppercase()}", value) }
            instrument.dimensions.forEach { dimension -> IndexBar(dimension.label, evolution.indices.idd[dimension.id]) }
        }
    }
}

@Composable
private fun IndexBar(label: String, value: Double?) {
    Text("$label: ${indexLabel(value)}", style = MaterialTheme.typography.bodySmall)
    if (value != null) LinearProgressIndicator(progress = { (value / 100).toFloat() }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EvaluationHistoryCard(record: EvaluationRecord, instrument: EvaluationInstrument) {
    var expanded by remember(record.id) { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${record.studentName} · ${record.classGroup}", style = MaterialTheme.typography.titleMedium)
            Text("${instrument.moments.labelFor(record.moment)} · ${record.evaluationDate} · IDG ${indexLabel(record.indices.idg)}")
            OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Recolher avaliação" else "Ver avaliação") }
            if (expanded) evaluationRecordLines(record, instrument).forEach { Text(it) }
        }
    }
}
