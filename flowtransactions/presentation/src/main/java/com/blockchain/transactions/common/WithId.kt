package com.blockchain.transactions.common

import java.util.UUID

data class WithId<T>(
    val id: String,
    val data: T,
)

fun <T> T.withId() = WithId(
    id = UUID.randomUUID().toString(),
    data = this
)
