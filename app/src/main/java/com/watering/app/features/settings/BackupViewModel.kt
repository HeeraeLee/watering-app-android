package com.watering.app.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.watering.app.core.service.AuthService
import com.watering.app.core.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Loading : BackupUiState
    data class Success(val timestampMillis: Long) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val authService: AuthService,
    private val backupService: BackupService
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authService.currentUser

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    init {
        refreshLastBackupTimestamp()
    }

    private fun refreshLastBackupTimestamp() {
        val uid = authService.currentUser.value?.uid ?: return
        viewModelScope.launch {
            backupService.getLastBackupTimestamp(uid)?.let { _backupUiState.value = BackupUiState.Success(it) }
        }
    }

    fun signIn(activityContext: Context) {
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            authService.signIn(activityContext)
                .onSuccess { refreshLastBackupTimestamp() }
                .onFailure { _backupUiState.value = BackupUiState.Error("로그인에 실패했어요. 다시 시도해주세요") }
        }
    }

    fun signOut() {
        authService.signOut()
        _backupUiState.value = BackupUiState.Idle
    }

    fun backup() {
        val uid = authService.currentUser.value?.uid ?: return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            backupService.backup(uid)
                .onSuccess { _backupUiState.value = BackupUiState.Success(it) }
                .onFailure { _backupUiState.value = BackupUiState.Error("네트워크 연결을 확인해주세요") }
        }
    }

    fun restore() {
        val uid = authService.currentUser.value?.uid ?: return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            backupService.restore(uid)
                .onSuccess { refreshLastBackupTimestamp() }
                .onFailure { _backupUiState.value = BackupUiState.Error("복원에 실패했어요. 네트워크 연결을 확인해주세요") }
        }
    }
}
