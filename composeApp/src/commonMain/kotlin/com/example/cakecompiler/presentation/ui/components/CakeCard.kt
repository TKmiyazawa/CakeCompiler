package com.example.cakecompiler.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cakecompiler.presentation.model.DisplayCake

// 選択時: LavenderBlush（柔らかなパステル）
private val SelectedBg = Color(0xFFFFF0F5)
// 選択時の枠線: 温かみのあるパステルコーラル
private val SelectedBorderColor = Color(0xFFFFAB91)
// デフォルト背景: 白に近い薄グレー
private val DefaultBg = Color(0xFFFAFAFA)

@Composable
fun CakeCard(
    cake: DisplayCake,
    description: String,
    isSelected: Boolean,
    isOverriding: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- 静寂の強調: 動きではなく「状態の変化」でシグナルを伝える ---

    // Scale: 選択カードは 1.03倍にふんわり拡大、非選択は通常サイズ
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.03f
            isOverriding -> 0.97f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 200)
    )

    // Alpha: 非選択カードはノイズ低減
    val alpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isOverriding -> 0.7f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 200)
    )

    // Elevation: 選択カードのみわずかに影で浮き上がる
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        animationSpec = tween(durationMillis = 200)
    )

    // Background: 選択時は LavenderBlush へじんわり変化
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> SelectedBg
            else -> DefaultBg
        },
        animationSpec = tween(durationMillis = 300)
    )

    // Border color: 選択時はパステルコーラルへ
    val borderColor by animateColorAsState(
        targetValue = when {
            cake.isSerendipityPick -> Color(0xFF7C4DFF)
            isSelected -> SelectedBorderColor
            cake.isRecommended -> MaterialTheme.colorScheme.secondary
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300)
    )

    val borderWidth = when {
        cake.isRecommended || isSelected || cake.isSerendipityPick -> 2.dp
        else -> 0.dp
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable { onTap() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // ランク表示（静止、シンプルな色変化のみ）
            Text(
                text = "#${cake.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> SelectedBorderColor
                    cake.isRecommended -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                }
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cake.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cake.isRecommended) {
                        Text(
                            text = "推奨",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (cake.isSerendipityPick) {
                        Text(
                            text = "意外な一品",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7C4DFF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                // 確率バー
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(cake.probability.toFloat())
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (cake.isRecommended)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                        )
                    }

                    Text(
                        text = "${(cake.probability * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Text(
                        text = "AIの予測を上書き中",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SelectedBorderColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (cake.partnerWouldLoveIt) {
                    Text(
                        text = "♥ パートナー向き",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAD1457).copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
