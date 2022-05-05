package com.blockchain.metadata

import com.blockchain.serialization.JsonSerializable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface MetadataRepository {

    fun <T : JsonSerializable> loadMetadata(
        metadataType: Int,
        adapter: KSerializer<T>,
        clazz: Class<T>
    ): Maybe<T>

    fun <T : JsonSerializable> saveMetadata(
        data: T,
        clazz: Class<T>,
        adapter: KSerializer<T>,
        metadataType: Int
    ): Completable
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : JsonSerializable> MetadataRepository.load(metadataType: Int): Maybe<T> =
    loadMetadata(
        metadataType,
        T::class.serializer(),
        T::class.java
    )

@OptIn(InternalSerializationApi::class)
inline fun <reified T : JsonSerializable> MetadataRepository.save(data: T, metadataType: Int): Completable =
    saveMetadata(
        data,
        T::class.java,
        T::class.serializer(),
        metadataType
    )
