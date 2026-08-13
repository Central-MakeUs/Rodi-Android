package com.dororong.rodi.feature.home.course

import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.usecase.place.GetRelatedSearchUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRegistrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `query calls related search after debounce and preserves api region strings`() = runTest(dispatcher) {
        val repository = mockk<PlaceRepository>()
        coEvery {
            repository.relatedSearch("중구", null, 20)
        } returns RelatedSearch(
            regions = listOf("세종특별자치시", "서울특별시 중구"),
            places = CursorPage(
                items = listOf(PlaceSuggestion(1L, "중구 연습 코스", "서울특별시 중구")),
                hasNext = false,
                nextCursor = null,
                totalCount = 1,
            ),
        )
        val viewModel = CourseRegistrationViewModel(GetRelatedSearchUseCase(repository))

        viewModel.onIntent(CourseRegistrationIntent.OnQueryChange(" 중구 "))
        assertEquals(CourseRegistrationSearchResultState.Loading, viewModel.state.value.resultState)
        advanceTimeBy(299)
        coVerify(exactly = 0) { repository.relatedSearch(any(), any(), any()) }

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(CourseRegistrationSearchResultState.Content, viewModel.state.value.resultState)
        assertEquals(
            listOf("세종특별자치시", "서울특별시 중구"),
            viewModel.state.value.regionSuggestions,
        )
        assertEquals(listOf(1L), viewModel.state.value.placeSuggestions.map(PlaceSuggestion::placeId))
        coVerify(exactly = 1) { repository.relatedSearch("중구", null, 20) }
    }

    @Test
    fun `blank query clears suggestions without calling api`() = runTest(dispatcher) {
        val repository = mockk<PlaceRepository>()
        val viewModel = CourseRegistrationViewModel(GetRelatedSearchUseCase(repository))

        viewModel.onIntent(CourseRegistrationIntent.OnQueryChange(" "))
        advanceUntilIdle()

        assertEquals(CourseRegistrationSearchResultState.Idle, viewModel.state.value.resultState)
        assertEquals(emptyList<String>(), viewModel.state.value.regionSuggestions)
        coVerify(exactly = 0) { repository.relatedSearch(any(), any(), any()) }
    }

    @Test
    fun `authentication failure explains that login is required`() = runTest(dispatcher) {
        val repository = mockk<PlaceRepository>()
        coEvery { repository.relatedSearch("중구", null, 20) } throws
            PlaceException.AuthenticationRequired("로그인이 필요합니다.")
        val viewModel = CourseRegistrationViewModel(GetRelatedSearchUseCase(repository))

        viewModel.onIntent(CourseRegistrationIntent.OnQueryChange("중구"))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("로그인 후 장소 검색을 사용할 수 있어요.", viewModel.state.value.errorMessage)
    }
}
