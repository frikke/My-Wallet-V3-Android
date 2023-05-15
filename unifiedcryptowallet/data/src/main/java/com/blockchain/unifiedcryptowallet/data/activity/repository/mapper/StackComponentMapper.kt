package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.StackComponentDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent

internal fun StackComponentDto.toStackComponent(): StackComponent? = when (this) {
    is StackComponentDto.Text -> StackComponent.Text(
        value = value,
        style = style.toTextStyle()
    )
    is StackComponentDto.Tag -> StackComponent.Tag(
        value = value,
        style = style.toTagStyle()
    )
    StackComponentDto.Unknown -> null
}
