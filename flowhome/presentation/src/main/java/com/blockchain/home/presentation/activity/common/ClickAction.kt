package com.blockchain.home.presentation.activity.common

import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction

sealed interface ClickAction {
    data class ButtonClick(
        val action: ActivityButtonAction
    ) : ClickAction

    data class TableRowClick(
        val data: String
    ) : ClickAction
}