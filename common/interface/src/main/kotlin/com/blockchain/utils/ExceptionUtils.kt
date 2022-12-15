package com.blockchain.utils

fun Throwable.toException() = when (this) {
    is Exception -> this
    else -> Exception(this)
}
