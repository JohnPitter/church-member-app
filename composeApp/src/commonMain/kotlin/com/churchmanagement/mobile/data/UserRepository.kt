package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

class UserRepository(private val firestore: FirebaseFirestore) {

    /** Adiciona o token FCM do dispositivo ao documento do usuário (para push). */
    suspend fun addFcmToken(uid: String, token: String) {
        firestore.collection(Collections.USERS)
            .document(uid)
            .update("fcmTokens" to FieldValue.arrayUnion(token))
    }
}
