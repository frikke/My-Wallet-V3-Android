package com.blockchain.home.presentation.activity.common

import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction

sealed interface ClickAction {
    data class Stack(
        val data: String
    ) : ClickAction

    data class Button(
        val action: ActivityButtonAction
    ) : ClickAction

    object None : ClickAction
}