package com.blockchain.extensions

inline fun <reified T : Enum<T>> enumValueOfOrNull(type: String, ignoreCase: Boolean = false): T? =
    if (ignoreCase) {
        enumValues<T>().firstOrNull {
            it.name.equals(type, ignoreCase = true)
        }
    } else {
        enumValueOf<T>(type)
    }
