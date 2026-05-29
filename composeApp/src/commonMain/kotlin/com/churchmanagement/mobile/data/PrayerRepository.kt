package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.model.CreatePrayerRequestDto
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp

class PrayerRepository(private val firestore: FirebaseFirestore) {

    /** Envia um novo pedido de oração (status inicial: pendente de aprovação). */
    suspend fun submit(
        name: String,
        request: String,
        isUrgent: Boolean,
        isAnonymous: Boolean,
    ) {
        val now = Timestamp.now()
        val dto = CreatePrayerRequestDto(
            name = if (isAnonymous) "Anônimo" else name,
            request = request.trim(),
            isUrgent = isUrgent,
            isAnonymous = isAnonymous,
            createdAt = now,
            updatedAt = now,
        )
        firestore.collection(Collections.PRAYER_REQUESTS).add(dto)
    }
}
