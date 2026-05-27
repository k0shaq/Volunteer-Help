package com.example.volunteerhelp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): FirebaseUser {
        return auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Не вдалося виконати вхід")
    }

    suspend fun register(email: String, password: String): FirebaseUser {
        return auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Не вдалося створити акаунт")
    }

    fun signOut() {
        auth.signOut()
    }
}
