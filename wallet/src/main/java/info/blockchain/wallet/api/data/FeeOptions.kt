package info.blockchain.wallet.api.data

import info.blockchain.balance.CryptoCurrency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FeeOptions constructor(
    /**
     * Returns a "gasLimit" for Ethereum
     */
    @SerialName("gasLimit")
    val gasLimit: Long = 0,

    /**
     * Returns a "regular" fee, which should result in a transaction being included in a block
     * within the next 4-6 hours. The fee is in Satoshis per byte.
     */
    @SerialName("regular")
    val regularFee: Long = 0,
    /**
     * Returns a "gasLimit" for Erc20 contract
     */
    @SerialName("gasLimitContract")
    val gasLimitContract: Long = 0,

    /**
     * Returns a "priority" fee, which should result in a transaction being included in a block in
     * an hour or so. The fee is in Satoshis per byte.
     */
    @SerialName("priority")
    val priorityFee: Long = 0,

    /**
     * Returns a "priority" fee, which should result in a transaction being included in a block in
     * an hour or so. The fee is in Satoshis per byte.
     */
    @SerialName("limits")
    val limits: FeeLimits? = null
) {

    companion object {

        /**
         * @return the default FeeOptions for XLM.
         */
        fun defaultForXlm(): FeeOptions {
            return FeeOptions(
                priorityFee = 100,
                regularFee = 100
            )
        }

        /**
         * @return the default FeeOptions for Ethereum.
         */
        fun defaultForEth(): FeeOptions {
            return FeeOptions(
                gasLimit = 21000,
                priorityFee = 23,
                regularFee = 23,
                gasLimitContract = 65000,
                limits = FeeLimits(23, 23)
            )
        }

        /**
         * @return the default FeeOptions for Bitcoin.
         */
        fun defaultForBtc(): FeeOptions {
            return FeeOptions(
                priorityFee = 11,
                regularFee = 5,
                limits = FeeLimits(2, 16)
            )
        }

        fun defaultForBch(): FeeOptions {
            return FeeOptions(
                regularFee = 4,
                priorityFee = 4
            )
        }

        fun defaultForErc20(): FeeOptions = defaultForEth()

        fun defaultForEvm(networkTicker: String): FeeOptions {
            return if (networkTicker == CryptoCurrency.AVAX.networkTicker) {
                // Fees for Avalanche are significantly higher than for other EVM networks
                FeeOptions(
                    gasLimit = 25000,
                    priorityFee = 35,
                    regularFee = 26,
                    gasLimitContract = 65000,
                    limits = FeeLimits(26, 35)
                )
            } else {
                defaultForEth()
            }
        }
    }
}
