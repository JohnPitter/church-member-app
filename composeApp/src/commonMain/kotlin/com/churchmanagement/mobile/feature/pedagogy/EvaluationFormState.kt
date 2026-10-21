package com.churchmanagement.mobile.feature.pedagogy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.churchmanagement.mobile.util.currentLocalDate
import kotlin.random.Random

class EvaluationFormState {
    private val today = currentLocalDate()
    var rollId by mutableStateOf("")
    var student by mutableStateOf("")
    var age by mutableStateOf("")
    var cycle by mutableStateOf(1)
    var moment by mutableStateOf("initial")
    var period by mutableStateOf("${today.year}-${if (today.monthNumber <= 6) 1 else 2}")
    var date by mutableStateOf(today.toString())
    var answers by mutableStateOf<Map<String, String>>(emptyMap())
    var observations by mutableStateOf(mapOf("attention" to "", "potential" to "", "additional" to ""))
    var referrals by mutableStateOf(listOf("none"))
    var otherReferral by mutableStateOf("")
    var requestId = newRequestId()
        private set
    private var previousRequest: EvaluationRequest? = null

    fun request(organization: String): EvaluationRequest {
        val request = EvaluationRequest(
            requestId, organization, rollId, student, age.toIntOrNull() ?: -1, cycle,
            moment, period, date, answers, observations, referrals, otherReferral,
        )
        if (previousRequest != null && previousRequest != request) requestId = newRequestId()
        return request.copy(requestId = requestId).also { previousRequest = it }
    }

    fun saved() {
        requestId = newRequestId()
        previousRequest = null
        answers = emptyMap()
        observations = mapOf("attention" to "", "potential" to "", "additional" to "")
        referrals = listOf("none")
        otherReferral = ""
    }

    private fun newRequestId() = "mobile-${Random.nextLong().toULong().toString(16)}-${Random.nextLong().toULong().toString(16)}"
}
