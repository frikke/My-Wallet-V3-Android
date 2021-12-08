package info.blockchain.wallet.exceptions

class DeterministicWalletException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    constructor(cause: Throwable) : this(null, cause)
    constructor(message: String) : this(message, null)
}
