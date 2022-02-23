package info.blockchain.wallet.ethereum

object EthUrls {
    /* Base endpoint for all ETH operations */
    private const val ETH = "eth"

    /* Base endpoint for v2 ETH operations */
    private const val ETHV2 = "v2/eth"

    /* Additional paths for certain queries */
    const val IS_CONTRACT = "/isContract"
    private const val DATA = "/data"

    /* Complete paths */
    const val ACCOUNT = "$ETH/account"
    const val PUSH_TX = "$ETH/pushtx"
    const val V2_DATA = ETHV2 + DATA
    const val V2_DATA_ACCOUNT = "$ETHV2$DATA/account"
    const val V2_DATA_TRANSACTION = "$ETHV2$DATA/transaction"
}
