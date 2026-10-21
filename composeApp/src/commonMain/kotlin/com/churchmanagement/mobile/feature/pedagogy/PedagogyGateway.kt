package com.churchmanagement.mobile.feature.pedagogy

interface PedagogyGateway {
    suspend fun workspace(request: WorkspaceRequest): PedagogyWorkspace
    suspend fun report(filter: EvaluationFilter): EvaluationReport
    suspend fun create(request: EvaluationRequest): EvaluationRecord
}
