package com.blockchain.metadata

import com.blockchain.serialization.JsonSerializable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal class MetadataRepositoryAdapter(
    private val metadataManager: MetadataManager,
    private val json: Json
) : MetadataRepository {

    override fun <T : JsonSerializable> loadMetadata(
        metadataType: MetadataEntry,
        adapter: KSerializer<T>,
        clazz: Class<T>
    ): Maybe<T> =
        metadataManager.fetchMetadata(metadataType.index)
            .map {
                json.decodeFromString(adapter, it)
            }
            .subscribeOn(Schedulers.io())

    override fun <T : JsonSerializable> saveMetadata(
        data: T,
        clazz: Class<T>,
        adapter: KSerializer<T>,
        metadataType: MetadataEntry
    ): Completable =
        metadataManager.saveToMetadata(
            json.encodeToString(adapter, data),
            metadataType.index
        )
            .subscribeOn(Schedulers.io())

    override fun saveRawValue(data: String, metadataType: MetadataEntry): Completable =
        metadataManager.saveToMetadata(
            data,
            metadataType.index
        )
            .subscribeOn(Schedulers.io())

    override fun loadRawValue(metadataType: MetadataEntry): Maybe<String> = metadataManager.fetchMetadata(
        metadataType.index
    ).subscribeOn(Schedulers.io())
}
