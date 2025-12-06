package com.example.h2now

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

// Create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository to manage user preferences using DataStore.
 */
class UserPreferencesRepository(private val context: Context) {
    private val dailyGoalKey = intPreferencesKey("daily_water_goal")
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val reminderAlertsKey = booleanPreferencesKey("reminder_alerts_enabled")

    private val defaultDailyGoal = 2000

    val dailyGoal: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[dailyGoalKey] ?: defaultDailyGoal
        }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[darkModeKey] ?: false
        }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[notificationsKey] ?: true
        }

    val reminderAlertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[reminderAlertsKey] ?: true
        }

    suspend fun updateDailyGoal(newGoal: Int) {
        context.dataStore.edit { settings ->
            settings[dailyGoalKey] = newGoal
        }
    }

    suspend fun setDarkMode(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[darkModeKey] = isEnabled
        }
    }

    suspend fun setNotifications(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[notificationsKey] = isEnabled
        }
    }

    suspend fun setReminderAlerts(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[reminderAlertsKey] = isEnabled
        }
    }
}

/**
 * A simple repository to manage water intake data.
 */
class WaterRepository {
    private val _records = MutableStateFlow<List<WaterIntakeRecord>>(emptyList())
    val records: StateFlow<List<WaterIntakeRecord>> = _records.asStateFlow()

    fun addRecord(record: WaterIntakeRecord) {
        // Add to the beginning of the list to show newest first
        _records.value = listOf(record) + _records.value
    }
}

/**
 * ViewModel for the water intake screen.
 */
class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {
    val records: StateFlow<List<WaterIntakeRecord>> = waterRepository.records

    // Expose daily goal from preferences
    val dailyGoal: StateFlow<Int> = prefsRepository.dailyGoal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2000 // A default initial value
        )

    val darkModeEnabled: StateFlow<Boolean> = prefsRepository.darkModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val notificationsEnabled: StateFlow<Boolean> = prefsRepository.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val reminderAlertsEnabled: StateFlow<Boolean> = prefsRepository.reminderAlertsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )


    fun addWaterIntake(amount: Double) {
        if (amount > 0) {
            val newRecord = WaterIntakeRecord(amount, Date())
            waterRepository.addRecord(newRecord)
        }
    }

    // Function to update the goal
    fun setDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            prefsRepository.updateDailyGoal(newGoal)
        }
    }

    fun setDarkMode(isEnabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setDarkMode(isEnabled)
        }
    }

    fun setNotifications(isEnabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setNotifications(isEnabled)
        }
    }

    fun setReminderAlerts(isEnabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setReminderAlerts(isEnabled)
        }
    }
}

/**
 * Factory for creating a WaterViewModel with repository dependencies.
 */
class WaterViewModelFactory(
    private val waterRepository: WaterRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(waterRepository, prefsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
