package com.example.h2now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface TipsApiService {
    @GET("data/hydration_tips.json")
    suspend fun getTips(): HydrationTipsResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://raw.githubusercontent.com/cheryl7114/mobile_CA3/main/"

    val api: TipsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TipsApiService::class.java)
    }
}

class TipsRepository {
    suspend fun getTips(): HydrationTipsResponse = RetrofitInstance.api.getTips()
}

sealed class TipsUiState {
    object Loading : TipsUiState()
    data class Success(val tips: List<Tip>) : TipsUiState()
    data class Error(val message: String) : TipsUiState()
}

class TipsViewModel(private val tipsRepository: TipsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<TipsUiState>(TipsUiState.Loading)
    val uiState: StateFlow<TipsUiState> = _uiState.asStateFlow()

    init {
        fetchTips()
    }

    private fun fetchTips() {
        viewModelScope.launch {
            _uiState.value = TipsUiState.Loading
            try {
                val response = tipsRepository.getTips()
                _uiState.value = TipsUiState.Success(response.tips)
            } catch (e: Exception) {
                _uiState.value = TipsUiState.Error("Failed to load tips: ${e.message}")
            }
        }
    }
}

class TipsViewModelFactory(private val repository: TipsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TipsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TipsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
