package com.example.h2now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

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
class WaterViewModel(private val repository: WaterRepository) : ViewModel() {
    val records: StateFlow<List<WaterIntakeRecord>> = repository.records

    fun addWaterIntake(amount: Double) {
        if (amount > 0) {
            val newRecord = WaterIntakeRecord(amount, Date())
            repository.addRecord(newRecord)
        }
    }
}

/**
 * Factory for creating a WaterViewModel with a repository dependency.
 */
class WaterViewModelFactory(private val repository: WaterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
