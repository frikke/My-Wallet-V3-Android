package com.blockchain.domain.common.model

data class PromotionStyleInfo(
    val title: String,
    val message: String,
    val iconUrl: String,
    val headerUrl: String,
    val backgroundUrl: String,
    val foregroundColorScheme: List<Float>,
    val actions: List<ServerErrorAction>
)
