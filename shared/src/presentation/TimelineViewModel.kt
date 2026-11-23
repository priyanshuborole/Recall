package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.util.toMonthHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class TimelineSection(val header: String, val memories: List<Memory>)

data class TimelineUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val sections: List<TimelineSection> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
}

class TimelineViewModel(
    private val repository: MemoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val results = query
        .debounce(250)
        .flatMapLatest { repository.observeTimeline(it) }

    val uiState: StateFlow<TimelineUiState> = combine(query, results) { q, memories ->
        TimelineUiState(
            isLoading = false,
            query = q,
            sections = memories
                .groupBy { it.occurredAt.toMonthHeader() }
                .map { (header, items) -> TimelineSection(header, items) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}
