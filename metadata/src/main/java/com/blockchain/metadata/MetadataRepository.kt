package com.blockchain.metadata

import com.blockchain.serialization.JsonSerializable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface MetadataRepository {

    fun <T : JsonSerializable> loadMetadata(
        metadataType: MetadataEntry,
        adapter: KSerializer<T>,
        clazz: Class<T>
    ): Maybe<T>

    fun <T : JsonSerializable> saveMetadata(
        data: T,
        clazz: Class<T>,
        adapter: KSerializer<T>,
        metadataType: MetadataEntry
    ): Completable

    fun saveRawValue(
        data: String,
        metadataType: MetadataEntry
    ): Completable

    fun loadRawValue(
        metadataType: MetadataEntry
    ): Maybe<String>
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : JsonSerializable> MetadataRepository.load(metadataType: MetadataEntry): Maybe<T> =
    loadMetadata(
        metadataType,
        T::class.serializer(),
        T::class.java
    )

@OptIn(InternalSerializationApi::class)
inline fun <reified T : JsonSerializable> MetadataRepository.save(data: T, metadataType: MetadataEntry): Completable =
    saveMetadata(
        data,
        T::class.java,
        T::class.serializer(),
        metadataType
    )
