package com.blockchain.store.impl

import com.blockchain.store.Parser
import java.lang.Exception
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonParser<T>(
    private val json: Json,
    private val serializer: KSerializer<T>
) : Parser<T> {
    override fun encode(data: T): String = json.encodeToString(serializer, data)

    override fun decode(data: String): T? = try {
        json.decodeFromString(serializer, data)
    } catch (ex: Exception) {
        null
    }
}
