package com.blockchain.store

interface Parser<T> {
    fun encode(data: T): String
    fun decode(data: String): T?
}