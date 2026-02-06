package com.example.cakecompiler.presentation.viewmodel

import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.presentation.model.CakeUiEvent
import com.example.cakecompiler.presentation.model.ScreenState
import com.example.cakecompiler.presentation.model.SerendipityMode
import com.example.cakecompiler.usecase.CakeCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CakeSelectionViewModelTest {

    private fun createViewModel() = CakeSelectionViewModel()

    private fun sampleCandidates() = listOf(
        CakeCandidate("cake1", "Chocolate Cake", PreferenceVector(0.9, 0.1, 0.6, 0.3, 0.8)),
        CakeCandidate("cake2", "Cheesecake", PreferenceVector(0.7, 0.4, 0.8, 0.2, 0.7)),
        CakeCandidate("cake3", "Fruit Tart", PreferenceVector(0.4, 0.6, 0.5, 0.5, 0.9))
    )

    @Test
    fun `initial state is correct`() {
        val viewModel = createViewModel()

        val state = viewModel.state.value

        assertEquals(ScreenState.Initial, state.screenState)
        assertTrue(state.cakes.isEmpty())
    }

    @Test
    fun `initialize sets up state correctly`() {
        val viewModel = createViewModel()
        val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.4, 0.7)
        val partnerPref = PreferenceVector(0.6, 0.3, 0.7, 0.3, 0.8)

        viewModel.initialize(selfPref, partnerPref, sampleCandidates())

        val state = viewModel.state.value
        assertTrue(state.screenState is ScreenState.Ready)
        assertEquals(3, state.cakes.size)
    }

    @Test
    fun `cakes are ranked by happiness score`() {
        val viewModel = createViewModel()
        val selfPref = PreferenceVector(0.9, 0.1, 0.6, 0.3, 0.8)
        val partnerPref = PreferenceVector(0.9, 0.1, 0.6, 0.3, 0.8)

        viewModel.initialize(selfPref, partnerPref, sampleCandidates())

        val state = viewModel.state.value
        val ranks = state.cakes.map { it.rank }

        assertEquals(listOf(1, 2, 3), ranks.sorted())
    }

    @Test
    fun `recommended cake is marked correctly`() {
        val viewModel = createViewModel()
        val selfPref = PreferenceVector.neutral()
        val partnerPref = PreferenceVector.neutral()

        viewModel.initialize(selfPref, partnerPref, sampleCandidates())

        val state = viewModel.state.value
        val recommended = state.cakes.filter { it.isRecommended }

        assertEquals(1, recommended.size)
        assertEquals(1, recommended.first().rank)
    }

    @Test
    fun `tapping recommended cake triggers acceptance`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector.neutral(),
            PreferenceVector.neutral(),
            sampleCandidates()
        )

        val recommendedId = (viewModel.state.value.screenState as ScreenState.Ready).recommendedCakeId
        viewModel.onEvent(CakeUiEvent.CakeTapped(recommendedId))

        val state = viewModel.state.value
        assertTrue(state.screenState is ScreenState.Completed)
        assertTrue(!(state.screenState as ScreenState.Completed).wasOverride)
    }

    @Test
    fun `tapping non-recommended cake prepares override`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector.neutral(),
            PreferenceVector.neutral(),
            sampleCandidates()
        )

        val recommendedId = (viewModel.state.value.screenState as ScreenState.Ready).recommendedCakeId
        val otherCakeId = sampleCandidates().first { it.id != recommendedId }.id

        viewModel.onEvent(CakeUiEvent.CakeTapped(otherCakeId))

        val state = viewModel.state.value
        assertTrue(state.screenState is ScreenState.Overriding)
    }

    @Test
    fun `confirm override completes with override flag`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector.neutral(),
            PreferenceVector.neutral(),
            sampleCandidates()
        )

        val recommendedId = (viewModel.state.value.screenState as ScreenState.Ready).recommendedCakeId
        val otherCakeId = sampleCandidates().first { it.id != recommendedId }.id

        viewModel.onEvent(CakeUiEvent.CakeTapped(otherCakeId))
        viewModel.onEvent(CakeUiEvent.ConfirmOverride(otherCakeId))

        val state = viewModel.state.value
        assertTrue(state.screenState is ScreenState.Completed)
        assertTrue((state.screenState as ScreenState.Completed).wasOverride)
    }

    @Test
    fun `shake detected activates serendipity mode`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            sampleCandidates()
        )

        viewModel.onEvent(CakeUiEvent.ShakeDetected)

        val state = viewModel.state.value
        assertTrue(state.serendipityMode is SerendipityMode.Active)
    }

    @Test
    fun `shake serendipity marks divergent cake`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            sampleCandidates()
        )

        viewModel.onEvent(CakeUiEvent.ShakeDetected)

        val state = viewModel.state.value
        val serendipityPicks = state.cakes.filter { it.isSerendipityPick }

        assertEquals(1, serendipityPicks.size)
    }

    @Test
    fun `dismiss serendipity resets mode`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0),
            sampleCandidates()
        )
        viewModel.onEvent(CakeUiEvent.ShakeDetected)

        viewModel.onEvent(CakeUiEvent.DismissSerendipity)

        val state = viewModel.state.value
        assertEquals(SerendipityMode.Off, state.serendipityMode)
        assertTrue(state.cakes.none { it.isSerendipityPick })
    }

    @Test
    fun `mathematical integrity - happiness scores are consistent`() {
        val viewModel = createViewModel()
        val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.4, 0.7)
        val partnerPref = PreferenceVector(0.6, 0.3, 0.7, 0.3, 0.8)

        viewModel.initialize(selfPref, partnerPref, sampleCandidates())

        val state = viewModel.state.value
        val scores = state.cakes.sortedBy { it.rank }.map { it.happinessScore.totalScore }

        // Verify scores are in descending order (higher rank = higher score)
        for (i in 0 until scores.size - 1) {
            assertTrue(scores[i] >= scores[i + 1],
                "Score at rank ${i + 1} should be >= score at rank ${i + 2}")
        }
    }

    @Test
    fun `mathematical integrity - touch events do not affect happiness calculation`() {
        val viewModel = createViewModel()
        val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.4, 0.7)
        val partnerPref = PreferenceVector(0.6, 0.3, 0.7, 0.3, 0.8)

        viewModel.initialize(selfPref, partnerPref, sampleCandidates())

        // Get initial scores
        val initialScores = viewModel.state.value.cakes.associate {
            it.id to it.happinessScore.totalScore
        }

        // Trigger touch events
        viewModel.onEvent(CakeUiEvent.CakeTouchStart("cake1"))
        viewModel.onEvent(CakeUiEvent.CakeTouchEnd)

        // Scores should remain unchanged
        val finalScores = viewModel.state.value.cakes.associate {
            it.id to it.happinessScore.totalScore
        }

        assertEquals(initialScores, finalScores)
    }

    @Test
    fun `status message updates appropriately`() {
        val viewModel = createViewModel()

        viewModel.initialize(
            PreferenceVector.neutral(),
            PreferenceVector.neutral(),
            sampleCandidates()
        )

        val state = viewModel.state.value
        assertNotNull(state.statusMessage)
    }

    @Test
    fun `retry resets state`() {
        val viewModel = createViewModel()
        viewModel.initialize(
            PreferenceVector.neutral(),
            PreferenceVector.neutral(),
            sampleCandidates()
        )
        viewModel.onEvent(CakeUiEvent.AcceptRecommendation)

        viewModel.onEvent(CakeUiEvent.Retry)

        val state = viewModel.state.value
        assertEquals(ScreenState.Initial, state.screenState)
    }
}
