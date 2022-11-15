package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle

internal fun String.toTagStyle(): ActivityTagStyle {
    val success = "successBadge"
    val warning = "Warning"

    return when (this) {
        success -> ActivityTagStyle.Success
        warning -> ActivityTagStyle.Warning
        else -> ActivityTagStyle.Success // todo what's the default here
    }
}
