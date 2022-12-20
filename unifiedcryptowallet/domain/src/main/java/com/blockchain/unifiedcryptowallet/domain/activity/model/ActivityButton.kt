package com.blockchain.unifiedcryptowallet.domain.activity.model

// button
enum class ActivityButtonStyle {
    Primary,
    Secondary,
    Tertiary
}

data class ActivityButtonAction(
    val type: ActivityButtonActionType,
    val data: String
) {
    enum class ActivityButtonActionType {
        Copy, OpenUrl
    }
}
