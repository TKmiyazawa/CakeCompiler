package com.example.cakecompiler.presentation.model

import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.usecase.CakeCandidate

/**
 * デモ用のモックデータ。
 * 実際のアプリではGemini APIや永続データに置き換わる。
 */
object MockCakeData {

    val selfPreference = PreferenceVector(
        sweetness = 0.7,
        sourness = 0.3,
        texture = 0.5,
        temperature = 0.4,
        artistry = 0.6
    )

    val partnerPreference = PreferenceVector(
        sweetness = 0.5,
        sourness = 0.2,
        texture = 0.6,
        temperature = 0.2,
        artistry = 0.9
    )

    val candidates: List<CakeCandidate> = listOf(
        CakeCandidate(
            id = "mont-blanc",
            name = "モンブラン",
            vector = PreferenceVector(0.7, 0.1, 0.6, 0.3, 0.9)
        ),
        CakeCandidate(
            id = "strawberry-short",
            name = "苺ショートケーキ",
            vector = PreferenceVector(0.8, 0.3, 0.4, 0.2, 0.7)
        ),
        CakeCandidate(
            id = "tiramisu",
            name = "ティラミス",
            vector = PreferenceVector(0.5, 0.2, 0.7, 0.2, 0.6)
        ),
        CakeCandidate(
            id = "cheese-tart",
            name = "ベイクドチーズタルト",
            vector = PreferenceVector(0.4, 0.5, 0.8, 0.4, 0.5)
        ),
        CakeCandidate(
            id = "matcha-parfait",
            name = "抹茶パフェ",
            vector = PreferenceVector(0.3, 0.2, 0.5, 0.1, 0.8)
        ),
        CakeCandidate(
            id = "chocolate-fondant",
            name = "フォンダンショコラ",
            vector = PreferenceVector(0.9, 0.0, 0.3, 0.9, 0.7)
        ),
        CakeCandidate(
            id = "fruit-tart",
            name = "フルーツタルト",
            vector = PreferenceVector(0.5, 0.6, 0.7, 0.2, 1.0)
        ),
        CakeCandidate(
            id = "mille-crepe",
            name = "ミルクレープ",
            vector = PreferenceVector(0.6, 0.1, 0.9, 0.3, 0.6)
        )
    )

    val geminiProbabilities: Map<String, Double> = mapOf(
        "mont-blanc" to 0.92,
        "fruit-tart" to 0.78,
        "strawberry-short" to 0.65,
        "matcha-parfait" to 0.58,
        "mille-crepe" to 0.45,
        "tiramisu" to 0.38,
        "cheese-tart" to 0.22,
        "chocolate-fondant" to 0.15
    )

    val descriptions: Map<String, String> = mapOf(
        "mont-blanc" to "栗の濃厚な風味と繊細な見た目",
        "strawberry-short" to "王道の甘さと苺の酸味のハーモニー",
        "tiramisu" to "ほろ苦い大人の味わい",
        "cheese-tart" to "酸味とコクの絶妙なバランス",
        "matcha-parfait" to "和の苦味と冷たさの融合",
        "chocolate-fondant" to "とろける温かいチョコレート",
        "fruit-tart" to "彩り鮮やかな果実の宝石箱",
        "mille-crepe" to "幾層にも重なるクレープの食感"
    )
}
