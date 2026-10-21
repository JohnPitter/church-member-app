package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EvaluationSubmission(private val gateway: PedagogyGateway, private val scope: CoroutineScope) {
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun submit(form: EvaluationFormState, organization: String) {
        if (saving) return
        val request = form.request(organization)
        saving = true; error = null; message = null
        scope.launch {
            try {
                val saved = gateway.create(request)
                message = "Avaliação de ${saved.studentName} registrada. Consulte os resultados em Relatórios."
                form.saved()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { error = failure.message ?: "Não foi possível salvar. Tente novamente."
            } finally { saving = false }
        }
    }
}
