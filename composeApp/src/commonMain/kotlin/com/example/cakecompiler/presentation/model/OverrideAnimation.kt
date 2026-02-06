package com.example.cakecompiler.presentation.model

/**
 * "The Heroic Override" - 英雄的オーバーライド
 *
 * ユーザーがAIの推奨を却下し、自らの直感で決定を下す際のアニメーション。
 * AIの予測値が「シュン…」と消え、ユーザーの決断が黄金色のエフェクトと共に確定される。
 */
sealed class OverrideAnimation {

    /**
     * アニメーションなし（待機状態）
     */
    data object Idle : OverrideAnimation()

    /**
     * オーバーライド開始 - AIの予測が消えていく
     * 「シュン…」という儚さを表現
     */
    data class AiFading(
        val progress: Float,  // 0.0 -> 1.0
        val originalCakeId: String,
        val originalCakeName: String
    ) : OverrideAnimation() {
        /**
         * AIカードの透明度（消えていく）
         */
        val aiCardAlpha: Float
            get() = 1.0f - progress

        /**
         * AIカードのスケール（小さくなりながら消える）
         */
        val aiCardScale: Float
            get() = 1.0f - (progress * 0.3f)

        /**
         * AIカードのY方向オフセット（下に沈んでいく）
         */
        val aiCardOffsetY: Float
            get() = progress * FADE_OFFSET_Y

        companion object {
            private const val FADE_OFFSET_Y = 20f
        }
    }

    /**
     * ユーザーの決断が確定 - 黄金色のエフェクト
     */
    data class UserDecisionConfirmed(
        val progress: Float,  // 0.0 -> 1.0
        val chosenCakeId: String,
        val chosenCakeName: String
    ) : OverrideAnimation() {
        /**
         * 黄金エフェクトの輝き強度
         */
        val goldenGlowIntensity: Float
            get() = if (progress < 0.5f) {
                progress * 2f  // 前半: 0 -> 1
            } else {
                1f - ((progress - 0.5f) * 0.6f)  // 後半: 1 -> 0.7（余韻を残す）
            }

        /**
         * 選択カードのスケール（確信を表現）
         */
        val cardScale: Float
            get() = 1.0f + (SCALE_BOOST * goldenGlowIntensity)

        /**
         * パルスリングの半径
         */
        val pulseRingRadius: Float
            get() = progress * MAX_RING_RADIUS

        /**
         * パルスリングの透明度
         */
        val pulseRingAlpha: Float
            get() = (1f - progress).coerceAtLeast(0f)

        companion object {
            private const val SCALE_BOOST = 0.1f
            private const val MAX_RING_RADIUS = 100f
        }
    }

    /**
     * アニメーション完了
     */
    data class Completed(
        val chosenCakeId: String,
        val wasHeroicOverride: Boolean
    ) : OverrideAnimation()
}

/**
 * オーバーライドアニメーションの状態管理
 */
data class OverrideAnimationState(
    val animation: OverrideAnimation = OverrideAnimation.Idle,
    val shouldTriggerHaptic: Boolean = false
) {
    val isAnimating: Boolean
        get() = animation !is OverrideAnimation.Idle && animation !is OverrideAnimation.Completed

    /**
     * オーバーライドアニメーションを開始
     */
    fun startOverride(
        originalCakeId: String,
        originalCakeName: String
    ): OverrideAnimationState = copy(
        animation = OverrideAnimation.AiFading(
            progress = 0f,
            originalCakeId = originalCakeId,
            originalCakeName = originalCakeName
        )
    )

    /**
     * フェードアニメーションの進行を更新
     */
    fun updateFadeProgress(progress: Float): OverrideAnimationState {
        val current = animation as? OverrideAnimation.AiFading ?: return this
        return copy(animation = current.copy(progress = progress.coerceIn(0f, 1f)))
    }

    /**
     * ユーザー決定確定フェーズへ移行
     */
    fun transitionToConfirmation(
        chosenCakeId: String,
        chosenCakeName: String
    ): OverrideAnimationState = copy(
        animation = OverrideAnimation.UserDecisionConfirmed(
            progress = 0f,
            chosenCakeId = chosenCakeId,
            chosenCakeName = chosenCakeName
        ),
        shouldTriggerHaptic = true  // ト・クンの鼓動
    )

    /**
     * 確定アニメーションの進行を更新
     */
    fun updateConfirmationProgress(progress: Float): OverrideAnimationState {
        val current = animation as? OverrideAnimation.UserDecisionConfirmed ?: return this
        return copy(
            animation = current.copy(progress = progress.coerceIn(0f, 1f)),
            shouldTriggerHaptic = false
        )
    }

    /**
     * アニメーション完了
     */
    fun complete(): OverrideAnimationState {
        val chosenId = when (val anim = animation) {
            is OverrideAnimation.UserDecisionConfirmed -> anim.chosenCakeId
            is OverrideAnimation.AiFading -> ""
            else -> ""
        }
        return copy(
            animation = OverrideAnimation.Completed(
                chosenCakeId = chosenId,
                wasHeroicOverride = true
            )
        )
    }

    /**
     * 状態をリセット
     */
    fun reset(): OverrideAnimationState = copy(
        animation = OverrideAnimation.Idle,
        shouldTriggerHaptic = false
    )
}

/**
 * 黄金色のカラー定義
 */
object GoldenColors {
    const val PRIMARY = 0xFFD4AF37       // 基本の金色
    const val LIGHT = 0xFFF5E6A3         // 明るい金色（グロー用）
    const val DARK = 0xFFB8860B          // 深い金色（影用）
    const val SHIMMER = 0xFFFFD700       // キラキラ用

    /**
     * 輝度に基づいた金色を補間
     */
    fun interpolate(intensity: Float): Long {
        return when {
            intensity < 0.5f -> lerp(DARK, PRIMARY, intensity * 2f)
            else -> lerp(PRIMARY, LIGHT, (intensity - 0.5f) * 2f)
        }
    }

    private fun lerp(start: Long, end: Long, fraction: Float): Long {
        val startA = (start shr 24) and 0xFF
        val startR = (start shr 16) and 0xFF
        val startG = (start shr 8) and 0xFF
        val startB = start and 0xFF

        val endA = (end shr 24) and 0xFF
        val endR = (end shr 16) and 0xFF
        val endG = (end shr 8) and 0xFF
        val endB = end and 0xFF

        val a = (startA + (endA - startA) * fraction).toLong()
        val r = (startR + (endR - startR) * fraction).toLong()
        val g = (startG + (endG - startG) * fraction).toLong()
        val b = (startB + (endB - startB) * fraction).toLong()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
