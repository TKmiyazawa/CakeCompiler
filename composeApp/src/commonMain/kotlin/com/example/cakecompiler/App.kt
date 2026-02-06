package com.example.cakecompiler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.cakecompiler.presentation.model.MockCakeData
import com.example.cakecompiler.presentation.ui.CakeCompilerTheme
import com.example.cakecompiler.presentation.ui.CakeSelectionScreen
import com.example.cakecompiler.presentation.viewmodel.CakeSelectionViewModel

@Composable
@Preview
fun App() {
    CakeCompilerTheme {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            CakeSelectionViewModel(coroutineScope = scope)
        }

        LaunchedEffect(Unit) {
            viewModel.initialize(
                selfPref = MockCakeData.selfPreference,
                partnerPref = MockCakeData.partnerPreference,
                cakeCandidates = MockCakeData.candidates,
                probabilities = MockCakeData.geminiProbabilities,
                descriptions = MockCakeData.descriptions
            )
        }

        CakeSelectionScreen(viewModel = viewModel)
    }
}
