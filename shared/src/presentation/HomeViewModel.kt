package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class HomeUiState(
    val isLoading: Boolean = true,
    val now: Instant? = null,
    val onThisDay: Memory? = null,
    val recent: List<Memory> = emptyList(),
)

class HomeViewModel(
    private val repository: MemoryRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecent(limit = 3),
        repository.observeOnThisDay(),
    ) { recent, onThisDay ->
        HomeUiState(
            isLoading = false,
            now = clock.now(),
            onThisDay = onThisDay.firstOrNull(),
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun saveTextMemory(text: String, mood: Mood?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.create(MemoryDraft(body = text, type = MemoryType.TEXT, mood = mood))
        }
    }
}
