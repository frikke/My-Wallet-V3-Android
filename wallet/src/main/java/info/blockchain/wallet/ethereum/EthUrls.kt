package info.blockchain.wallet.ethereum

object EthUrls {
    /* Base endpoint for all ETH operations */
    private const val ETH = "eth"

    /* Base endpoint for v2 ETH operations */
    private const val ETHV2 = "v2/eth"

    /* Additional paths for certain queries */
    private const val DATA = "/data"

    /* Complete paths */
    const val ACCOUNT = "$ETH/account"
    const val V2_DATA_ACCOUNT = "$ETHV2$DATA/account"
    const val ETH_NODES = "$ETH/nodes/rpc"
}
