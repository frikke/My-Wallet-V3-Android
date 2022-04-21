package piuk.blockchain.androidcore.data.metadata

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataRepository
import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@OptIn(InternalSerializationApi::class)
internal class MoshiMetadataRepositoryAdapter(
    private val metadataManager: MetadataManager,
    private val moshi: Moshi,
    private val json: Json,
    private val disableMoshiFeatureFlag: FeatureFlag
) : MetadataRepository {

    override fun <T : JsonSerializable> loadMetadata(
        metadataType: Int,
        adapter: KSerializer<T>,
        clazz: Class<T>
    ): Maybe<T> =
        metadataManager.fetchMetadata(metadataType)
            .map {
                if (disableMoshiFeatureFlag.isEnabled) {
                    json.decodeFromString(adapter, it)
                } else {
                    adapter(clazz).fromJson(it) ?: throw IllegalStateException("Error parsing JSON")
                }
            }
            .subscribeOn(Schedulers.io())

    override fun <T : JsonSerializable> saveMetadata(
        data: T,
        clazz: Class<T>,
        adapter: KSerializer<T>,
        metadataType: Int
    ): Completable =
        disableMoshiFeatureFlag.enabled.flatMapCompletable { isMoshiDisabled ->
            metadataManager.saveToMetadata(
                if (isMoshiDisabled) json.encodeToString(adapter, data) else adapter(clazz).toJson(data),
                metadataType
            )
        }
            .subscribeOn(Schedulers.io())

    private fun <T : JsonSerializable> adapter(clazz: Class<T>) = moshi.adapter(clazz)
}
