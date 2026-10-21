package com.churchmanagement.mobile.platform

import com.churchmanagement.mobile.feature.pedagogy.EvaluationDocument

interface ReportExporter {
    val formats: List<String>
    suspend fun export(document: EvaluationDocument, format: String)
}
