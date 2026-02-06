package com.example.cakecompiler.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cakecompiler.presentation.model.DisplayCake

// バナーの暖色背景（上書き中）
private val OverrideBannerBg = Color(0xFFFFF3E0)

@Composable
fun RecommendationBanner(
    topCake: DisplayCake?,
    isOverriding: Boolean,
    modifier: Modifier = Modifier
) {
    if (topCake == null) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // バナー背景色をスムーズに変化
    val containerColor by animateColorAsState(
        targetValue = if (isOverriding) OverrideBannerBg
        else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(durationMillis = 400)
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                // タイトル: クロスフェードで切り替え
                Crossfade(
                    targetState = isOverriding,
                    animationSpec = tween(durationMillis = 350)
                ) { overriding ->
                    Text(
                        text = if (overriding)
                            "あなたの手動選択（AI予測を上書き中）"
                        else
                            "Gemini AI 推奨",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overriding)
                            Color(0xFFE65100).copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${(topCake.probability * 100).toInt()}%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "の確率で",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                Text(
                    text = topCake.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.height(12.dp))

                // 幸福度バー
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = topCake.happinessMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(6.dp))

                    val score = topCake.happinessScore.totalScore
                    val barFraction = (score / 2.5).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }

                    if (topCake.partnerWouldLoveIt) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "♥ パートナーも喜ぶ選択",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAD1457)
                        )
                    }
                }
            }
        }
    }
}
