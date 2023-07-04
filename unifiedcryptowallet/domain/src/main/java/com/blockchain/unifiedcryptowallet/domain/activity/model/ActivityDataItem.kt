package com.blockchain.unifiedcryptowallet.domain.activity.model

import com.blockchain.image.LogoValue

sealed interface ActivityDataItem {
    data class Stack(
        val leadingImage: LogoValue,
        val leading: List<StackComponent>,
        val trailing: List<StackComponent>
    ) : ActivityDataItem

    data class Button(
        val value: String,
        val style: ActivityButtonStyle,
        val action: ActivityButtonAction
    ) : ActivityDataItem
}
