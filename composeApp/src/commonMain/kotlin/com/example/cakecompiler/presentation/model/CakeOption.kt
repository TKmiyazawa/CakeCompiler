package com.example.cakecompiler.presentation.model

/**
 * UIリスト表示用のケーキオプション。
 * DisplayCake から動的に生成される。
 */
data class CakeOption(
    val name: String,
    val description: String,
    val matchProbability: Double,
    val isPartnerPreference: Boolean,
    val rank: Int
)

/**
 * DisplayCake → CakeOption の変換
 */
fun DisplayCake.toCakeOption(): CakeOption = CakeOption(
    name = name,
    description = description,
    matchProbability = probability,
    isPartnerPreference = partnerWouldLoveIt,
    rank = rank
)
