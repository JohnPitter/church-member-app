package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.domain.AppUser
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(private val auth: FirebaseAuth) {

    /** Emite o usuário atual (ou null) sempre que o estado de autenticação muda. */
    val authState: Flow<AppUser?> = auth.authStateChanged.map { it?.toAppUser() }

    val currentUser: AppUser? get() = auth.currentUser?.toAppUser()

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password)
    }

    suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toAppUser() = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoURL,
)
