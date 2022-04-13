package com.blockchain.mapper

interface Mapper<in A, out B> {
    fun map(type: A): B
}
