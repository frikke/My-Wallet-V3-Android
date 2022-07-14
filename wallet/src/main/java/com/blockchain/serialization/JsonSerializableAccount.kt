package com.blockchain.serialization

interface JsonSerializableAccount : JsonSerializable {
    val label: String
    fun updateArchivedState(isArchived: Boolean): JsonSerializableAccount
    val isArchived: Boolean
}
