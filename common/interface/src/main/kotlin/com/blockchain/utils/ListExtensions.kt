package com.blockchain.utils

fun <T> MutableList<T>.replaceInList(replacement: T, where: (T) -> Boolean) {
    this[indexOf(find(where))] = replacement
}