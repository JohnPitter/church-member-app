package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

class EvaluationReportState(private val gateway: PedagogyGateway, private val filter: EvaluationFilter) {
    var report by mutableStateOf<EvaluationReport?>(null)
        private set
    var details by mutableStateOf<EvaluationReport?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    suspend fun loadSummary(onLoaded: (EvaluationReport) -> Unit) {
        error = null
        try {
            val result = gateway.report(filter)
            report = result
            onLoaded(result)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { error = failure.message ?: "Não foi possível carregar os relatórios." }
    }

    suspend fun loadDetails(onLoaded: suspend (EvaluationReport) -> Unit) {
        if (busy) return
        busy = true; error = null
        try {
            val complete = details ?: gateway.report(filter.copy(includeDetails = true))
            details = complete
            onLoaded(complete)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { error = failure.message ?: "Não foi possível concluir a solicitação."
        } finally { busy = false }
    }
}
