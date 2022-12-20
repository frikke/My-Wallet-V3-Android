package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle

internal fun String.toTagStyle(): ActivityTagStyle {
    val default = "default"
    val success = "green"
    val info = "info"
    val warning = "primary"
    val error = "error"

    return when (this) {
        default -> ActivityTagStyle.Default
        success -> ActivityTagStyle.Success
        info -> ActivityTagStyle.Info
        warning -> ActivityTagStyle.Warning
        error -> ActivityTagStyle.Error
        else -> ActivityTagStyle.Warning
    }
}
