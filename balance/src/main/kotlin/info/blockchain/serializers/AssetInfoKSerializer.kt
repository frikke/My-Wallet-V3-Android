package info.blockchain.serializers

import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AssetInfoKSerializer(private val assetCatalogue: AssetCatalogue) : KSerializer<AssetInfo>, KoinComponent {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AssetInfo", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AssetInfo) {
        encoder.encodeString(value.networkTicker)
    }

    override fun deserialize(decoder: Decoder): AssetInfo {
        return assetCatalogue.assetInfoFromNetworkTicker(decoder.decodeString())
            ?: error("Unknown Asset ticker")
    }
}
