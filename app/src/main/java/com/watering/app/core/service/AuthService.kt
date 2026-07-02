package com.watering.app.core.service

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.watering.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// 백업/복원 기능에서만 쓰는 선택적 로그인 — 온보딩과 무관, 앱은 로그인 없이도 완전히 동작한다
@Singleton
class AuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth -> _currentUser.value = auth.currentUser }
    }

    // activityContext는 반드시 Activity Context여야 한다 (Credential Manager 시스템 UI 표시용)
    suspend fun signIn(activityContext: Context): Result<FirebaseUser> = runCatching {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        firebaseAuth.signInWithCredential(firebaseCredential).await().user
            ?: error("로그인한 사용자 정보를 가져오지 못했습니다")
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
