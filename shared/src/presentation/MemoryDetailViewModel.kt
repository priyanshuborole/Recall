package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MemoryDetailUiState {
    data object Loading : MemoryDetailUiState
    data object NotFound : MemoryDetailUiState
    data class Loaded(val memory: Memory) : MemoryDetailUiState
}

class MemoryDetailViewModel(
    private val memoryId: String,
    private val repository: MemoryRepository,
) : ViewModel() {

    val uiState: StateFlow<MemoryDetailUiState> = repository.observeMemory(memoryId)
        .map { memory -> memory?.let { MemoryDetailUiState.Loaded(it) } ?: MemoryDetailUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryDetailUiState.Loading)

    fun setHidden(hidden: Boolean) {
        viewModelScope.launch { repository.setHidden(memoryId, hidden) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(memoryId)
            onDeleted()
        }
    }
}
