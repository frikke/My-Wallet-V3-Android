package info.blockchain.wallet.ethereum

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Erc20TokenData {

    @SerialName("label")
    var label: String = ""

    @SerialName("contract")
    var contractAddress: String = ""
        private set

    @SerialName("has_seen")
    var hasSeen: Boolean = false

    @SerialName("tx_notes")
    val txNotes: HashMap<String, String> = HashMap()

    fun putTxNote(txHash: String, txNote: String) {
        txNotes[txHash] = txNote
    }

    fun removeTxNote(txHash: String) {
        txNotes.remove(txHash)
    }

    fun clearTxNotes() {
        txNotes.clear()
    }

    fun hasLabelAndAddressStored(): Boolean =
        contractAddress.isNotBlank() && label.isNotBlank()

    companion object {
        fun createTokenData(asset: AssetInfo, label: String): Erc20TokenData {
            require(asset.l1chainTicker == CryptoCurrency.ETHER.networkTicker)
            require(asset.l2identifier != null)

            return Erc20TokenData().apply {
                contractAddress = asset.l2identifier!!
                this.label = label
            }
        }
    }
}
