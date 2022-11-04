package com.blockchain.unifiedcryptowallet.domain.activity.model

sealed interface ActivityDataItem {
    data class Stack(
        val leadingImage: ActivityIcon?,
        val leading: List<StackComponent>,
        val trailing: List<StackComponent>,
    ) : ActivityDataItem

    data class Button(
        val value: String,
        val style: ActivityButtonStyle,
        val action: ActivityButtonAction
    ) : ActivityDataItem
}
