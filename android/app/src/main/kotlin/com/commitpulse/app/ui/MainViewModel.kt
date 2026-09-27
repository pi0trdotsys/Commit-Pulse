package com.commitpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.github.GitHubAccount
import androidx.glance.appwidget.updateAll
import com.commitpulse.app.github.GitHubResult
import com.commitpulse.app.widget.CommitPulseWidget
import com.commitpulse.app.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<Application>().commitPulseApp()
    private val settingsRepository get() = app.settingsRepository
    private val gitHubRepository get() = app.gitHubRepository

    val settings: StateFlow<WidgetSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetSettings())

    val history: StateFlow<List<DayCommit>> = settingsRepository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val account: StateFlow<GitHubAccount?> = settingsRepository.accountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastError: StateFlow<String?> = settingsRepository.lastErrorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastSync: StateFlow<Long?> = settingsRepository.lastSyncFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val hasToken: Boolean get() = gitHubRepository.hasToken()

    init {
        if (hasToken) refresh()
    }

    fun updateSettings(transform: (WidgetSettings) -> WidgetSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
            CommitPulseWidget().updateAll(getApplication())
            val updated = settingsRepository.currentSettings()
            WorkScheduler.scheduleRefresh(getApplication(), updated)
            WorkScheduler.scheduleWeeklyDigest(getApplication(), updated)
            WorkScheduler.scheduleStreakAlert(getApplication(), updated)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = gitHubRepository.fetchHistory()) {
                is GitHubResult.Success -> {
                    settingsRepository.saveAccount(result.value.first)
                    settingsRepository.saveHistory(result.value.second)
                    CommitPulseWidget().updateAll(getApplication())
                }
                is GitHubResult.Failure -> settingsRepository.saveError(result.message)
            }
            _isSyncing.value = false
        }
    }

    fun signIn(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            _isSyncing.value = true
            gitHubRepository.saveToken(token)
            when (val result = gitHubRepository.fetchHistory()) {
                is GitHubResult.Success -> {
                    settingsRepository.saveAccount(result.value.first)
                    settingsRepository.saveHistory(result.value.second)
                    CommitPulseWidget().updateAll(getApplication())
                }
                is GitHubResult.Failure -> {
                    settingsRepository.saveError(result.message)
                    gitHubRepository.signOut()
                }
            }
            _isSyncing.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            gitHubRepository.signOut()
            settingsRepository.saveAccount(null)
            settingsRepository.saveHistory(emptyList())
            CommitPulseWidget().updateAll(getApplication())
        }
    }
}
