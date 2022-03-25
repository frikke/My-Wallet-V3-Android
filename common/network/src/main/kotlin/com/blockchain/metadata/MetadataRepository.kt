package com.blockchain.metadata

import com.blockchain.serialization.JsonSerializable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import kotlinx.serialization.KSerializer

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
