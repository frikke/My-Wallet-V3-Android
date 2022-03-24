package com.blockchain.serialization

import java.io.Serializable
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Useful for limiting the objects/classes IDE suggests serialization on as well as using in proguard
 */
interface JsonSerializable : Serializable

/**
 * Deserialize any [JsonSerializable] from a [String].
 */
@OptIn(InternalSerializationApi::class)
fun <T : JsonSerializable> KClass<T>.fromJson(input: String, json: Json? = null): T {
    json?.let {
        return it.decodeFromString(this.serializer(), input)
    } ?: return Json.decodeFromString(this.serializer(), input)
}

/**
 * Serialize any [JsonSerializable] to a [String].
 */
inline fun <reified T : JsonSerializable> T.toJson(adapter: KSerializer<T>, json: Json? = null): String {
    json?.let {
        return it.encodeToString(adapter, this)
    } ?: return Json.encodeToString(adapter, this)
}
