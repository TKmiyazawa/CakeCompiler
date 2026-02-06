package com.example.cakecompiler.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cakecompiler.presentation.model.CakeUiEffect
import com.example.cakecompiler.presentation.model.ScreenState
import com.example.cakecompiler.presentation.model.CakeUiEvent
import com.example.cakecompiler.presentation.ui.components.ButterflyEffectOverlay
import com.example.cakecompiler.presentation.ui.components.CakeCard
import com.example.cakecompiler.presentation.ui.components.OverrideButton
import com.example.cakecompiler.presentation.ui.components.PartnerStatement
import com.example.cakecompiler.presentation.ui.components.RecommendationBanner
import com.example.cakecompiler.presentation.ui.components.StatusBar
import com.example.cakecompiler.presentation.viewmodel.CakeSelectionViewModel

@Composable
fun CakeSelectionScreen(
    viewModel: CakeSelectionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // ButterflyEffect state
    var butterflyEffect by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Effect handling
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CakeUiEffect.ShowButterflyEffect -> {
                    butterflyEffect = effect.notification to effect.memoryMoment
                }
                is CakeUiEffect.ShowToast -> { /* handled by platform */ }
                is CakeUiEffect.PlayHaptic -> { /* handled by platform */ }
                is CakeUiEffect.Navigate -> { /* navigation */ }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().safeContentPadding()) {
        when (val screenState = state.screenState) {
            is ScreenState.Initial, is ScreenState.Loading -> {
                LoadingContent(
                    message = state.statusMessage?.text
                )
            }

            is ScreenState.Ready -> {
                CakeListContent(
                    state = state,
                    overridingCakeId = null,
                    onEvent = viewModel::onEvent
                )
            }

            is ScreenState.Overriding -> {
                CakeListContent(
                    state = state,
                    overridingCakeId = screenState.newCakeId,
                    onEvent = viewModel::onEvent
                )
            }

            is ScreenState.Completed -> {
                CompletedContent(
                    state = state,
                    wasOverride = screenState.wasOverride,
                    chosenCakeId = screenState.chosenCakeId,
                    onEvent = viewModel::onEvent
                )
            }

            is ScreenState.Error -> {
                ErrorContent(
                    message = screenState.message,
                    onRetry = { viewModel.onEvent(CakeUiEvent.Retry) }
                )
            }
        }

        // ButterflyEffect overlay
        AnimatedVisibility(
            visible = butterflyEffect != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            butterflyEffect?.let { (notification, moment) ->
                ButterflyEffectOverlay(
                    notification = notification,
                    memoryMoment = moment,
                    onDismiss = { butterflyEffect = null }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message ?: "読込中...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CakeListContent(
    state: com.example.cakecompiler.presentation.model.CakeDisplayState,
    overridingCakeId: String?,
    onEvent: (CakeUiEvent) -> Unit
) {
    val recommendedCake = state.cakes.firstOrNull { it.isRecommended }
    val selectedCake = overridingCakeId?.let { id -> state.cakes.find { it.id == id } }
    val isOverriding = overridingCakeId != null

    Column(modifier = Modifier.fillMaxSize()) {
        // Status message
        StatusBar(statusMessage = state.statusMessage)

        // Scrollable content
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // AI Recommendation banner (タイトルがクロスフェードで切り替わる)
            item {
                RecommendationBanner(
                    topCake = recommendedCake,
                    isOverriding = isOverriding
                )
                Spacer(Modifier.height(12.dp))
            }

            // Cake list (選択・非選択でアニメーション差別化)
            items(state.cakes, key = { it.id }) { cake ->
                CakeCard(
                    cake = cake,
                    description = cake.description,
                    isSelected = cake.id == overridingCakeId,
                    isOverriding = isOverriding,
                    onTap = { onEvent(CakeUiEvent.CakeTapped(cake.id)) }
                )
            }

            // Partner statement
            item {
                Spacer(Modifier.height(8.dp))
                PartnerStatement()
            }
        }

        // Override button (The Final Bit)
        OverrideButton(
            selectedCakeName = selectedCake?.name ?: "",
            visible = overridingCakeId != null,
            onConfirm = {
                overridingCakeId?.let { onEvent(CakeUiEvent.ConfirmOverride(it)) }
            }
        )
    }
}

@Composable
private fun CompletedContent(
    state: com.example.cakecompiler.presentation.model.CakeDisplayState,
    wasOverride: Boolean,
    chosenCakeId: String,
    onEvent: (CakeUiEvent) -> Unit
) {
    val chosenCake = state.cakes.find { it.id == chosenCakeId }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(
            text = if (wasOverride) "💫" else "🎂",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = chosenCake?.name ?: "",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = state.statusMessage?.text ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (wasOverride) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "AIを超えて、あなたの判断で選びました。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        PartnerStatement()

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { onEvent(CakeUiEvent.RestartSelection) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "もう一度、愛を演算する",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.TextButton(onClick = onRetry) {
            Text("もう一度試す")
        }
    }
}
