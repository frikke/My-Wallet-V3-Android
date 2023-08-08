package com.blockchain.betternavigation.utils

import java.util.UUID

object NonSerializableArgsHolder {
    private val lock = Unit
    private val map: MutableMap<UUID, Any?> = mutableMapOf()

    fun put(uuid: UUID, data: Any?) {
        synchronized(lock) {
            map[uuid] = data
        }
    }

    fun pop(uuid: UUID): Any? = synchronized(lock) {
        map.remove(uuid)
    }
}
