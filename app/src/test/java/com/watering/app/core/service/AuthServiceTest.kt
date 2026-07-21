package com.watering.app.core.service

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

// signIn()/deleteAccount()는 Credential Manager(androidx.credentials)·Google Identity SDK를
// 여러 겹의 static 팩토리(CredentialManager.create / GoogleIdTokenCredential.createFrom /
// GoogleAuthProvider.getCredential)로 거치는 코드라, 순수 MockK로 흉내내도 "우리 로직"보다 SDK
// 연동 자체를 재현하는 비중이 훨씬 커서 이번 범위에서 제외 — 이 경로는 실기기 테스트가 이미
// 검증해온 영역(CLAUDE.md 테스트 전략 참고). currentUser 초기값/AuthStateListener 반영, signOut()
// 위임만 검증한다.
class AuthServiceTest {

    private fun createService(firebaseAuth: FirebaseAuth, context: Context = mockk(relaxed = true)) =
        AuthService(context, firebaseAuth)

    @Test
    fun currentUser_생성시점firebaseAuth의currentUser를초기값으로갖는다() {
        val user = mockk<FirebaseUser>()
        val firebaseAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns user
        }

        val service = createService(firebaseAuth)

        assertEquals(user, service.currentUser.value)
    }

    @Test
    fun currentUser_authStateListener가발동하면최신값으로갱신된다() {
        val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
        val firebaseAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns null
            every { addAuthStateListener(capture(listenerSlot)) } returns Unit
        }
        val service = createService(firebaseAuth)
        val newUser = mockk<FirebaseUser>()
        val updatedAuth = mockk<FirebaseAuth> {
            every { currentUser } returns newUser
        }

        listenerSlot.captured.onAuthStateChanged(updatedAuth)

        assertEquals(newUser, service.currentUser.value)
    }

    @Test
    fun signOut_firebaseAuth에위임한다() {
        val firebaseAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns null
        }
        val service = createService(firebaseAuth)

        service.signOut()

        verify { firebaseAuth.signOut() }
    }
}
