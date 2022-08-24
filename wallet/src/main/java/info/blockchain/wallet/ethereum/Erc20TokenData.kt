package info.blockchain.wallet.ethereum

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Erc20TokenData(

    @SerialName("label")
    val label: String,

    @SerialName("contract")
    val contractAddress: String,

    @SerialName("has_seen")
    private val _hasSeen: Boolean? = null,

    @SerialName("tx_notes")
    private val _txNotes: Map<String, String>? = null
) {
    val txNotes: Map<String, String>
        get() = _txNotes ?: emptyMap()

    val hasSeen: Boolean
        get() = _hasSeen ?: false

    fun putTxNote(txHash: String, txNote: String): Erc20TokenData =
        copy(
            _txNotes = txNotes.plus(txHash to txNote)
        )

    fun removeTxNote(txHash: String): Erc20TokenData = copy(
        _txNotes = txNotes.minus(txHash)
    )

    fun clearTxNotes(txHash: String): Erc20TokenData = copy(
        _txNotes = emptyMap()
    )

    fun hasLabelAndAddressStored(): Boolean =
        contractAddress.isNotBlank() && label.isNotBlank()

    companion object {
        fun createTokenData(asset: AssetInfo, label: String): Erc20TokenData {
            require(asset.l1chainTicker == CryptoCurrency.ETHER.networkTicker)
            require(asset.l2identifier != null)

            return Erc20TokenData(
                contractAddress = asset.l2identifier!!,
                _hasSeen = false,
                label = label
            )
        }
    }
}
