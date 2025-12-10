package com.example.h2now

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

@Entity(tableName = "water_intake_records")
data class WaterIntakeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: Date
)

@Dao
interface WaterIntakeDao {
    @Query("SELECT * FROM water_intake_records ORDER BY date DESC")
    fun getAll(): Flow<List<WaterIntakeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterIntakeRecord)

    @Update
    suspend fun update(record: WaterIntakeRecord)

    @Delete
    suspend fun delete(record: WaterIntakeRecord)
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(entities = [WaterIntakeRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterIntakeDao(): WaterIntakeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


/**
 * A simple repository to manage water intake data.
 */
class WaterRepository(private val waterIntakeDao: WaterIntakeDao) {
    val records: Flow<List<WaterIntakeRecord>> = waterIntakeDao.getAll()

    suspend fun addRecord(record: WaterIntakeRecord) {
        Log.d(TAG, "Repository: inserting $record ml")
        waterIntakeDao.insert(record)
    }

    suspend fun updateRecord(record: WaterIntakeRecord) {
        Log.d(TAG, "Repository: updating record id=${record.id}")
        waterIntakeDao.update(record)
    }

    suspend fun deleteRecord(record: WaterIntakeRecord) {
        Log.d(TAG, "Repository: deleting record id=${record.id}")
        waterIntakeDao.delete(record)
    }
}

/**
 * ViewModel for the water intake screen.
 */
class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {
    val records: StateFlow<List<WaterIntakeRecord>> = waterRepository.records.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
        Log.d(TAG, "addWaterIntake: adding $amount ml")
        viewModelScope.launch {
            if (amount > 0) {
                val newRecord = WaterIntakeRecord(amount = amount, date = Date())
                waterRepository.addRecord(newRecord)
                Log.d(TAG, "addWaterIntake: insert complete")
            }
        }
    }

    fun updateWaterIntake(record: WaterIntakeRecord) {
        Log.d(TAG, "updateWaterIntake: updating record id=${record.id}, amount=${record.amount}")
        viewModelScope.launch {
            waterRepository.updateRecord(record)
            Log.d(TAG, "updateWaterIntake: update complete")
        }
    }

    fun deleteWaterIntake(record: WaterIntakeRecord) {
        Log.d(TAG, "deleteWaterIntake: deleting record id=${record.id}")
        viewModelScope.launch {
            waterRepository.deleteRecord(record)
            Log.d(TAG, "deleteWaterIntake: delete complete")
        }
    }

    fun setDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            prefsRepository.updateDailyGoal(newGoal)
            Log.d(TAG, "setDailyGoal: set goal to $newGoal ml")
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
