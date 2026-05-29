package com.churchmanagement.mobile.messaging

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Serviço FCM. Só precisamos do [onNewToken] para registrar o token renovado.
 * Notificações em segundo plano são exibidas automaticamente pelo SDK (notification messages);
 * em foreground, o observador do Firestore em App() já cuida da exibição.
 */
class AppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
    }
}
