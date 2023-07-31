package com.blockchain.betternavigation.utils

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.UUID

/**
 * Serializable wrapper for non Serializable classes, this will save, or bind, the class to the app process,
 * if the process is killed, as in process death, `data` will return null.
 */
class Bindable<T>(_data: T) : Serializable {
    @Transient
    var data: T? = _data
        private set

    private val uuid: UUID by lazy {
        UUID.randomUUID()
    }

    private fun writeObject(oss: ObjectOutputStream) {
        oss.defaultWriteObject()
        NonSerializableArgsHolder.put(uuid, data)
        oss.writeObject(uuid)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(ois: ObjectInputStream) {
        ois.defaultReadObject()
        val uuid = ois.readObject() as UUID
        data = NonSerializableArgsHolder.pop(uuid) as? T
    }
}
