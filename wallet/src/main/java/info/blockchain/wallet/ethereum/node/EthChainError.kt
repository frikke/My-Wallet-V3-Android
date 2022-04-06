package info.blockchain.wallet.ethereum.node

sealed class EthChainError(open val throwable: Throwable) {
    data class NetworkError(override val throwable: Throwable) : EthChainError(throwable)
    data class HttpError(override val throwable: Throwable) : EthChainError(throwable)
    data class UnknownError(override val throwable: Throwable) : EthChainError(throwable)
}
