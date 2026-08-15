package com.dororong.rodi.feature.mypage.registeredcourses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.domain.usecase.course.DeleteRegisteredCourseUseCase
import com.dororong.rodi.core.domain.usecase.course.GetMyRegisteredCoursesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

private data class RegisteredCoursePage(
    val items: List<RegisteredCourse> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null,
)

@HiltViewModel
class RegisteredCoursesViewModel @Inject constructor(
    private val getMyRegisteredCourses: GetMyRegisteredCoursesUseCase,
    private val deleteRegisteredCourse: DeleteRegisteredCourseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisteredCoursesUiState(isLoading = true))
    val uiState: StateFlow<RegisteredCoursesUiState> = _uiState.asStateFlow()

    private val pages = mutableMapOf<RegisteredCourseFilter, RegisteredCoursePage>()
    private var selectedFilter = RegisteredCourseFilter.ALL
    private var loadJob: Job? = null
    private var appendJob: Job? = null

    init {
        loadInitial()
    }

    fun selectFilter(filter: RegisteredCourseFilter) {
        if (selectedFilter == filter) return
        loadJob?.cancel()
        appendJob?.cancel()
        selectedFilter = filter
        val page = pages[filter]
        publish(page ?: RegisteredCoursePage(isLoading = true))
        if (page == null || !page.loaded) loadInitial(filter)
    }

    fun loadInitial(filter: RegisteredCourseFilter = selectedFilter) {
        loadJob?.cancel()
        selectedFilter = filter
        val current = pages[filter].orEmpty().copy(
            items = emptyList(),
            hasNext = false,
            nextCursor = null,
            loaded = false,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            appendErrorMessage = null,
        )
        pages[filter] = current
        publish(current)
        loadJob = viewModelScope.launch {
            getMyRegisteredCourses(
                status = filter.status,
                cursor = null,
                size = PAGE_SIZE,
            ).onSuccess { page ->
                val next = RegisteredCoursePage(
                    items = page.items.distinctBy(RegisteredCourse::courseId),
                    hasNext = page.hasNext && page.nextCursor != null,
                    nextCursor = page.nextCursor,
                    loaded = true,
                )
                pages[filter] = next
                if (selectedFilter == filter) publish(next)
            }.onFailure { error ->
                val next = current.copy(
                    loaded = true,
                    isLoading = false,
                    errorMessage = error.message ?: "등록한 코스를 불러오지 못했어요.",
                )
                pages[filter] = next
                if (selectedFilter == filter) publish(next)
            }
        }
    }

    fun loadNextPage() {
        val filter = selectedFilter
        val current = pages[filter] ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isLoading || current.isLoadingMore || appendJob?.isActive == true) return
        pages[filter] = current.copy(isLoadingMore = true, appendErrorMessage = null)
        publish(pages.getValue(filter))
        appendJob = viewModelScope.launch {
            getMyRegisteredCourses(status = filter.status, cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    val merged = (current.items + page.items).distinctBy(RegisteredCourse::courseId)
                    val next = pages.getValue(filter).copy(
                        items = merged,
                        hasNext = page.hasNext && page.nextCursor != null,
                        nextCursor = page.nextCursor,
                        isLoadingMore = false,
                        appendErrorMessage = null,
                    )
                    pages[filter] = next
                    if (selectedFilter == filter) publish(next)
                }.onFailure { error ->
                    val next = pages.getValue(filter).copy(
                        isLoadingMore = false,
                        appendErrorMessage = error.message ?: "다음 코스를 불러오지 못했어요.",
                    )
                    pages[filter] = next
                    if (selectedFilter == filter) publish(next)
                }
        }
    }

    fun retry() {
        if (_uiState.value.errorMessage != null && _uiState.value.courses.isEmpty()) {
            loadInitial()
        } else {
            clearError()
            loadNextPage()
        }
    }

    fun clearError() {
        pages[selectedFilter] = pages[selectedFilter].orEmpty().copy(
            errorMessage = null,
            appendErrorMessage = null,
        )
        publish(pages.getValue(selectedFilter))
    }

    fun delete(course: RegisteredCourse) {
        if (_uiState.value.deletingCourseId != null) return
        _uiState.update { it.copy(deletingCourseId = course.courseId, errorMessage = null) }
        viewModelScope.launch {
            deleteRegisteredCourse(course.courseId)
                .onSuccess {
                    pages.keys.toList().forEach { filter ->
                        pages[filter] = pages.getValue(filter).copy(
                            items = pages.getValue(filter).items.filterNot { it.courseId == course.courseId },
                        )
                    }
                    _uiState.update {
                        it.copy(
                            courses = it.courses.filterNot { item -> item.courseId == course.courseId },
                            deletingCourseId = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            deletingCourseId = null,
                            errorMessage = error.message ?: "코스를 삭제하지 못했어요.",
                        )
                    }
                }
        }
    }

    private fun publish(page: RegisteredCoursePage) {
        _uiState.value = RegisteredCoursesUiState(
            selectedFilter = selectedFilter,
            courses = page.items,
            isLoading = page.isLoading,
            isLoadingMore = page.isLoadingMore,
            hasNext = page.hasNext,
            nextCursor = page.nextCursor,
            errorMessage = page.errorMessage,
            appendErrorMessage = page.appendErrorMessage,
            deletingCourseId = _uiState.value.deletingCourseId,
        )
    }

    private fun RegisteredCoursePage?.orEmpty(): RegisteredCoursePage = this ?: RegisteredCoursePage()
}
