package com.blockchain.core.common.mapper

import com.blockchain.api.NabuUxErrorResponse
import com.blockchain.api.mapActions
import com.blockchain.domain.common.model.ServerSideUxErrorInfo

fun NabuUxErrorResponse.toDomain(): ServerSideUxErrorInfo = ServerSideUxErrorInfo(
    id = id,
    title = title,
    description = message,
    iconUrl = icon?.url.orEmpty(),
    statusUrl = icon?.status?.url.orEmpty(),
    actions = mapActions(),
    categories = categories ?: emptyList()
)
