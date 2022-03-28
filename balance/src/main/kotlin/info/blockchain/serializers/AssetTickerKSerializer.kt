package info.blockchain.serializers

import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AssetTickerKSerializer : KSerializer<AssetInfo>, KoinComponent {
    private val assetCatalogue: AssetCatalogue by inject()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AssetInfo", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AssetInfo) {
        encoder.encodeString(value.networkTicker)
    }

    override fun deserialize(decoder: Decoder): AssetInfo {
        return assetCatalogue.assetInfoFromNetworkTicker(decoder.decodeString())
            ?: error("Unknown Asset ticker")
    }
}