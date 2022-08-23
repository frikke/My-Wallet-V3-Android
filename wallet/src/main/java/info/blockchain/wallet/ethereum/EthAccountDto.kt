package info.blockchain.wallet.ethereum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.web3j.crypto.Keys

@Serializable
data class EthAccountDto(
    @SerialName("archived")
    private val _archived: Boolean? = null,
    @SerialName("label")
    val label: String,
    @SerialName("correct")
    private val _isCorrect: Boolean? = null,
    @SerialName("addr")
    val address: String
) {
    fun rename(label: String): EthAccountDto {
        return copy(label = label)
    }

    fun withChecksummedAddress(): EthAccountDto =
        copy(
            address = Keys.toChecksumAddress(address)
        )

    val archived: Boolean
        get() = _archived ?: false
    val isCorrect: Boolean
        get() = _isCorrect ?: false

    companion object {
        fun fromCheckSumAddress(address: String, label: String): EthAccountDto =
            EthAccountDto(
                address = address,
                label = label,
                _isCorrect = true,
                _archived = false,
            )
    }
}
