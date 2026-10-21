package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.ui.LoadingBox
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
fun EvaluationForm(organization: String, instrument: EvaluationInstrument, form: EvaluationFormState) {
    val repository: PedagogyGateway = koinInject()
    var rolls by remember(organization) { mutableStateOf<List<WorkspaceRecord>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val submission = remember(repository, scope) { EvaluationSubmission(repository, scope) }
    LaunchedEffect(organization, retry) {
        error = null
        try { rolls = repository.workspace(WorkspaceRequest(organization, "evaluation")).records
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { error = failure.message ?: "Não foi possível carregar as chamadas." }
    }
    if (submission.saving) { LoadingBox(); return }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Avaliação do desenvolvimento", style = MaterialTheme.typography.titleLarge) }
        item { Text("Registre o desenvolvimento, as potencialidades e o acompanhamento dos assistidos. Consulte os resultados em Relatórios.") }
        error?.let { item { PedagogyError(it) { retry++ } } }
        submission.message?.let { item { Text(it) } }
        submission.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (rolls == null && error == null) item { CircularProgressIndicator() }
        if (rolls?.isEmpty() == true) item { Text("Nenhuma chamada disponível. Registre a chamada da turma na versão web para vincular a avaliação.") }
        item { EvaluationIdentity(form, rolls.orEmpty(), instrument) }
        items(instrument.cycles.first { it.id == form.cycle }.questions, key = { it.dimensionId }) { question ->
            EvaluationQuestionField(question, instrument, form)
        }
        items(instrument.observations, key = { it.id }) { option ->
            OutlinedTextField(
                value = form.observations[option.id].orEmpty(),
                onValueChange = { form.observations = form.observations + (option.id to it) },
                label = { Text(option.label) }, supportingText = { Text(option.example) },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
        }
        item { EvaluationReferrals(instrument, form) }
        item {
            Button(enabled = !rolls.isNullOrEmpty(), onClick = { submission.submit(form, organization) }) { Text("Salvar avaliação") }
        }
    }
}

@Composable
private fun EvaluationQuestionField(question: EvaluationQuestion, instrument: EvaluationInstrument, form: EvaluationFormState) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Text(question.example, style = MaterialTheme.typography.bodySmall)
            PedagogySelect("Resposta", form.answers[question.dimensionId] ?: "", instrument.answers.map { it.id to it.label }) {
                form.answers = form.answers + (question.dimensionId to it)
            }
        }
    }
}

@Composable
private fun EvaluationIdentity(form: EvaluationFormState, rolls: List<WorkspaceRecord>, instrument: EvaluationInstrument) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PedagogySelect("Chamada e turma", form.rollId, rolls.sortedByDescending { it.sessionDate }.map {
            it.id to "${it.classGroup} · ${it.sessionDate}"
        }) { form.rollId = it; form.student = "" }
        PedagogySelect("Assistido", form.student, rolls.firstOrNull { it.id == form.rollId }?.students.orEmpty().map {
            it.name to it.name
        }) { form.student = it }
        OutlinedTextField(form.age, { form.age = it }, label = { Text("Idade") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        PedagogySelect("Ciclo", form.cycle.toString(), instrument.cycles.map { it.id.toString() to it.label }) {
            form.cycle = it.toInt(); form.answers = emptyMap()
        }
        PedagogySelect("Momento", form.moment, instrument.moments.map { it.id to it.label }) { form.moment = it }
        OutlinedTextField(form.period, { form.period = it }, label = { Text("Período (ano-semestre, ex.: 2026-2)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(form.date, { form.date = it }, label = { Text("Data da avaliação (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EvaluationReferrals(instrument: EvaluationInstrument, form: EvaluationFormState) {
    Column {
        Text("Necessidade de encaminhamento", style = MaterialTheme.typography.titleMedium)
        instrument.referrals.forEach { option ->
            Row {
                Checkbox(checked = option.id in form.referrals, onCheckedChange = { selected ->
                    form.referrals = when {
                        !selected -> form.referrals - option.id
                        option.id == "none" -> listOf("none")
                        else -> (form.referrals - "none") + option.id
                    }
                })
                Text(option.label, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if ("other" in form.referrals) OutlinedTextField(form.otherReferral, { form.otherReferral = it }, label = { Text("Outro encaminhamento") })
    }
}
