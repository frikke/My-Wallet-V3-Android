package com.blockchain.betternavigation

import java.io.Serializable

internal class NavigationArgsHolder internal constructor(
    initialMap: Map<String, Serializable> = mutableMapOf()
) {
    internal val backingField = initialMap.toMutableMap()

    val keys: Set<String>
        get() = backingField.keys

    operator fun get(key: String): Serializable? {
        return backingField[key]
    }

    operator fun set(key: String, value: Serializable?) {
        if (value != null) {
            backingField[key] = value
        }
    }

    fun remove(key: String): Serializable? {
        return backingField.remove(key)
    }
}
