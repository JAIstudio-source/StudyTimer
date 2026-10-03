package com.madeby.JAI

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



data class TimerUiState(
    val state: TimerState = TimerState.IDLE,
    val mode: String = "STOPWATCH",
    val focusCountdownSecs: Long = 1500L,
    val focusRemainingSecs: Long = 0L,
    val accumulatedStudy: Long = 0L,
    val currentBreakSeconds: Long = 0L,
    val prePauseState: TimerState = TimerState.STUDYING
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "accumulatedStudy" -> _uiState.value = _uiState.value.copy(accumulatedStudy = sharedPreferences.getLong(key, 0L))
            "currentBreakSeconds" -> _uiState.value = _uiState.value.copy(currentBreakSeconds = sharedPreferences.getLong(key, 0L))
            "timerMode" -> _uiState.value = _uiState.value.copy(mode = sharedPreferences.getString(key, "STOPWATCH") ?: "STOPWATCH")
            "focusCountdownSecs" -> _uiState.value = _uiState.value.copy(focusCountdownSecs = sharedPreferences.getLong(key, 1500L))
            "timerState" -> {
                val stateStr = sharedPreferences.getString(key, "IDLE") ?: "IDLE"
                _uiState.value = _uiState.value.copy(state = TimerState.valueOf(stateStr))
            }
        }
    }

    init {
        // Load initial state from SharedPreferences
        _uiState.value = _uiState.value.copy(
            accumulatedStudy = prefs.getLong("accumulatedStudy", 0L),
            currentBreakSeconds = prefs.getLong("currentBreakSeconds", 0L),
            mode = prefs.getString("timerMode", "STOPWATCH") ?: "STOPWATCH",
            focusCountdownSecs = prefs.getLong("focusCountdownSecs", 1500L)
        )
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setTimerMode(mode: String) {
        _uiState.value = _uiState.value.copy(mode = mode)
        prefs.edit().putString("timerMode", mode).apply()
    }

    fun setTimerState(state: TimerState) {
        _uiState.value = _uiState.value.copy(state = state)
        // Also persist state if needed
    }

    fun updateFocusRemaining(secs: Long) {
        _uiState.value = _uiState.value.copy(focusRemainingSecs = secs)
    }

    fun addAccumulatedStudy(secs: Long) {
        val newVal = _uiState.value.accumulatedStudy + secs
        _uiState.value = _uiState.value.copy(accumulatedStudy = newVal)
        prefs.edit().putLong("accumulatedStudy", newVal).apply()
    }

    fun addBreakSeconds(secs: Long) {
        val newVal = _uiState.value.currentBreakSeconds + secs
        _uiState.value = _uiState.value.copy(currentBreakSeconds = newVal)
        prefs.edit().putLong("currentBreakSeconds", newVal).apply()
    }

    fun resetRunningSessionAccumulators() {
        _uiState.value = _uiState.value.copy(
            accumulatedStudy = 0L,
            currentBreakSeconds = 0L
        )
        prefs.edit()
            .putLong("accumulatedStudy", 0L)
            .putLong("currentBreakSeconds", 0L)
            .apply()
    }
    
    fun pause(prePause: TimerState) {
        _uiState.value = _uiState.value.copy(
            state = TimerState.PAUSED,
            prePauseState = prePause
        )
    }
}
