package com.blockchain.domain.common.model

typealias Millis = Long
typealias Seconds = Long

fun Millis.toSeconds(): Seconds = this / 1000
fun Seconds.toMillis(): Millis = this * 1000
