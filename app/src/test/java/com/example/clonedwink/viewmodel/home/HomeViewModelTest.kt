package com.example.clonedwink.viewmodel.home

import com.example.clonedwink.data.model.home.HomeContent
import com.example.clonedwink.data.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Android/testing: same reasoning as LandingViewModelTest — HomeViewModel never touches the
// Android framework directly, so this runs as a fast JVM test under app/src/test.
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads home content from repository on init`() = runTest {
        val content = HomeContent(loyaltyPoints = 100, currentStationName = "Eunos")
        val viewModel = HomeViewModel(FakeHomeRepository(content))

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertEquals(content, state.content)
    }

    @Test
    fun `surfaces an error flag when the repository throws`() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(content = null))

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasError)
        assertEquals(HomeContent(), state.content)
    }

    private class FakeHomeRepository(private val content: HomeContent?) : HomeRepository {
        override suspend fun getHomeContent(): HomeContent =
            content ?: throw IllegalStateException("boom")
    }
}
