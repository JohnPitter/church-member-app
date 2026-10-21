package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.feature.pedagogy.EvaluationFilter
import com.churchmanagement.mobile.feature.pedagogy.EvaluationRecord
import com.churchmanagement.mobile.feature.pedagogy.EvaluationReport
import com.churchmanagement.mobile.feature.pedagogy.EvaluationRequest
import com.churchmanagement.mobile.feature.pedagogy.PedagogyWorkspace
import com.churchmanagement.mobile.feature.pedagogy.PedagogyGateway
import com.churchmanagement.mobile.feature.pedagogy.WorkspaceRequest
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CancellationException

class PedagogyRepository : PedagogyGateway {
    private val functions = Firebase.functions("southamerica-east1")

    override suspend fun workspace(request: WorkspaceRequest): PedagogyWorkspace = integration {
        functions.httpsCallable("getDevelopmentEvaluationWorkspace")(request).data()
    }

    override suspend fun report(filter: EvaluationFilter): EvaluationReport = integration {
        functions.httpsCallable("getDevelopmentEvaluationReport")(filter).data()
    }

    override suspend fun create(request: EvaluationRequest): EvaluationRecord = integration {
        functions.httpsCallable("createDevelopmentEvaluation")(request).data()
    }

    private suspend fun <T> integration(operation: suspend () -> T): T = try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        println("[Pedagogy] Falha na integração: ${failure::class.simpleName}")
        throw failure
    }
}
