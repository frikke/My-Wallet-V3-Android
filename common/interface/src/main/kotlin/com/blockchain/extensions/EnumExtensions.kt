package com.blockchain.extensions

inline fun <reified T : Enum<T>> enumValueOfOrNull(type: String, ignoreCase: Boolean = false): T? =
    enumValues<T>().firstOrNull {
        it.name.equals(type, ignoreCase)
    }
