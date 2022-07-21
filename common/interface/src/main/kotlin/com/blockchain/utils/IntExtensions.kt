package com.blockchain.utils

infix fun Int.isLastIn(list: List<*>): Boolean = this == list.size - 1

infix fun Int.isLastIn(array: Array<*>): Boolean = this == array.size - 1

infix fun Int.isLastIn(map: Map<*, *>): Boolean = this == map.size - 1

infix fun Int.isNotLastIn(list: List<*>): Boolean = isLastIn(list).not()

infix fun Int.isNotLastIn(array: Array<*>): Boolean = isLastIn(array).not()

infix fun Int.isNotLastIn(map: Map<*, *>): Boolean = isLastIn(map).not()
