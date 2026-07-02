package com.watering.app.features.settings

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.watering.app.core.service.AuthService
import com.watering.app.core.service.BackupService
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// BackupViewModel의 init 블록이 즉시 backupService를 호출하므로(UnconfinedTestDispatcher),
// 모든 테스트는 createViewModel()을 호출하기 전에 필요한 stub을 먼저 설정해야 한다.
class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var currentUserFlow: MutableStateFlow<FirebaseUser?>
    private lateinit var authService: AuthService
    private lateinit var backupService: BackupService

    private fun signedInUser(uid: String = "uid-1", email: String = "test@example.com") =
        mockk<FirebaseUser> {
            every { this@mockk.uid } returns uid
            every { this@mockk.email } returns email
        }

    @Before
    fun setUp() {
        currentUserFlow = MutableStateFlow(null)
        authService = mockk {
            every { currentUser } returns currentUserFlow
            every { signOut() } returns Unit
        }
        backupService = mockk()
    }

    private fun createViewModel(initialUser: FirebaseUser? = null): BackupViewModel {
        currentUserFlow.value = initialUser
        return BackupViewModel(authService, backupService)
    }

    @Test
    fun 초기상태는_로그아웃상태에서Idle이다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(initialUser = null)

        assertEquals(BackupUiState.Idle, viewModel.backupUiState.value)
    }

    @Test
    fun 초기상태_로그인되어있으면_마지막백업시각을조회한다() = runTest(mainDispatcherRule.testDispatcher) {
        val user = signedInUser()
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns 999L

        val viewModel = createViewModel(initialUser = user)

        assertEquals(BackupUiState.Success(999L), viewModel.backupUiState.value)
    }

    @Test
    fun signIn_성공하면_마지막백업시각을다시조회한다() = runTest(mainDispatcherRule.testDispatcher) {
        val user = signedInUser()
        val activity = mockk<Context>()
        coEvery { authService.signIn(activity) } answers {
            currentUserFlow.value = user
            Result.success(user)
        }
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns 500L
        val viewModel = createViewModel(initialUser = null)

        viewModel.signIn(activity)

        assertEquals(BackupUiState.Success(500L), viewModel.backupUiState.value)
    }

    @Test
    fun signIn_실패하면_Error상태가된다() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = mockk<Context>()
        coEvery { authService.signIn(activity) } returns Result.failure(RuntimeException("실패"))
        val viewModel = createViewModel(initialUser = null)

        viewModel.signIn(activity)

        assertTrue(viewModel.backupUiState.value is BackupUiState.Error)
    }

    @Test
    fun signOut_authService에위임하고Idle로돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns 100L
        val viewModel = createViewModel(initialUser = signedInUser())

        viewModel.signOut()

        verify { authService.signOut() }
        assertEquals(BackupUiState.Idle, viewModel.backupUiState.value)
    }

    @Test
    fun backup_로그인상태에서성공하면_Success상태로전환된다() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns null
        coEvery { backupService.backup("uid-1") } returns Result.success(12345L)
        val viewModel = createViewModel(initialUser = signedInUser())

        viewModel.backup()

        assertEquals(BackupUiState.Success(12345L), viewModel.backupUiState.value)
    }

    @Test
    fun backup_로그아웃상태면_아무동작하지않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(initialUser = null)

        viewModel.backup()

        coVerify(exactly = 0) { backupService.backup(any()) }
    }

    @Test
    fun backup_실패하면_Error상태가된다() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns null
        coEvery { backupService.backup("uid-1") } returns Result.failure(RuntimeException("네트워크 오류"))
        val viewModel = createViewModel(initialUser = signedInUser())

        viewModel.backup()

        assertTrue(viewModel.backupUiState.value is BackupUiState.Error)
    }

    @Test
    fun restore_성공하면_마지막백업시각을다시조회한다() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns null andThen 42L
        coEvery { backupService.restore("uid-1") } returns Result.success(Unit)
        val viewModel = createViewModel(initialUser = signedInUser())

        viewModel.restore()

        assertEquals(BackupUiState.Success(42L), viewModel.backupUiState.value)
    }

    @Test
    fun restore_실패하면_Error상태가된다() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { backupService.getLastBackupTimestamp("uid-1") } returns null
        coEvery { backupService.restore("uid-1") } returns Result.failure(RuntimeException("실패"))
        val viewModel = createViewModel(initialUser = signedInUser())

        viewModel.restore()

        assertTrue(viewModel.backupUiState.value is BackupUiState.Error)
    }

    @Test
    fun restore_로그아웃상태면_아무동작하지않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(initialUser = null)

        viewModel.restore()

        coVerify(exactly = 0) { backupService.restore(any()) }
    }
}
