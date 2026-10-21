package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
fun PedagogyScreen() {
    var organization by remember { mutableStateOf("church") }
    Column(Modifier.fillMaxSize()) {
        PedagogySelect("Contexto", organization, listOf("church" to "Igreja", "ong" to "ONG")) {
            organization = it
        }
        PedagogyWorkspaceScreen(organization)
    }
}

@Composable
private fun PedagogyWorkspaceScreen(organization: String) {
    val repository: PedagogyGateway = koinInject()
    var workspace by remember(organization) { mutableStateOf<PedagogyWorkspace?>(null) }
    var error by remember(organization) { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    var tab by remember(organization) { mutableStateOf("reports") }
    val form = remember(organization) { EvaluationFormState() }
    LaunchedEffect(organization, retry) {
        error = null
        try {
            workspace = repository.workspace(WorkspaceRequest(organization, "context"))
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { error = failure.message ?: "Não foi possível acessar a Pedagogia." }
    }
    error?.let { PedagogyError(it) { retry++ }; return }
    val context = workspace ?: run { CircularProgressIndicator(); return }
    val tabs = if (context.access.create) listOf("evaluation" to "Avaliação", "applications" to "Aplicação", "reports" to "Relatórios")
        else listOf("applications" to "Aplicação", "reports" to "Relatórios")
    ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) {
        tabs.forEach { (id, label) -> Tab(selected = tab == id, onClick = { tab = id }, text = { Text(label) }) }
    }
    when (tab) {
        "evaluation" -> EvaluationForm(organization, context.instrument, form)
        "applications" -> PedagogyApplications(organization)
        else -> EvaluationReports(organization, context.instrument)
    }
}

@Composable
private fun PedagogyApplications(organization: String) {
    val repository: PedagogyGateway = koinInject()
    var records by remember(organization) { mutableStateOf<List<WorkspaceRecord>?>(null) }
    var error by remember(organization) { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(organization, retry) {
        error = null
        try { records = repository.workspace(WorkspaceRequest(organization, "applications")).records
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { error = failure.message ?: "Não foi possível carregar as aplicações." }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Aplicação das diretrizes", style = MaterialTheme.typography.titleLarge)
            Text("Relatos dos arte-educadores sobre a aplicação das diretrizes. Os indicadores dos assistidos estão em Relatórios.")
        }
        error?.let { item { PedagogyError(it) { retry++ } } }
        if (records == null && error == null) item { CircularProgressIndicator() }
        if (records?.isEmpty() == true) item { Text("Nenhum relato de aplicação registrado neste contexto.") }
        items(records.orEmpty(), key = { it.id }) { record ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(record.guidelineTitle, style = MaterialTheme.typography.titleMedium)
                    Text("${record.educatorName} · ${record.month}/${record.year}")
                    Text("Relato: ${record.applicationNarrative}")
                    Text("Dificuldades na aplicação: ${record.applicationDifficulties}")
                    Text("Estratégias: ${record.strategies}")
                    Text("Resultados observados: ${record.observedResults}")
                }
            }
        }
    }
}
