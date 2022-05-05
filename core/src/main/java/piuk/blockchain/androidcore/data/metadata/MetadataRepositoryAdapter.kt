package piuk.blockchain.androidcore.data.metadata

import com.blockchain.metadata.MetadataRepository
import com.blockchain.serialization.JsonSerializable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@OptIn(InternalSerializationApi::class)
internal class MetadataRepositoryAdapter(
    private val metadataManager: MetadataManager,
    private val json: Json
) : MetadataRepository {

    override fun <T : JsonSerializable> loadMetadata(
        metadataType: Int,
        adapter: KSerializer<T>,
        clazz: Class<T>
    ): Maybe<T> =
        metadataManager.fetchMetadata(metadataType)
            .map {
                json.decodeFromString(adapter, it)
            }
            .subscribeOn(Schedulers.io())

    override fun <T : JsonSerializable> saveMetadata(
        data: T,
        clazz: Class<T>,
        adapter: KSerializer<T>,
        metadataType: Int
    ): Completable =
        metadataManager.saveToMetadata(
            json.encodeToString(adapter, data),
            metadataType
        )
            .subscribeOn(Schedulers.io())
}
