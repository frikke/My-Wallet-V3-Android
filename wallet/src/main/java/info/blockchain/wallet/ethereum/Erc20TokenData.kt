package info.blockchain.wallet.ethereum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Erc20TokenData(
    @SerialName("tx_notes")
    private val _txNotes: Map<String, String>? = null
) {
    val txNotes: Map<String, String>
        get() = _txNotes ?: emptyMap()

    fun putTxNote(txHash: String, txNote: String): Erc20TokenData =
        copy(
            _txNotes = txNotes.plus(txHash to txNote)
        )
}
