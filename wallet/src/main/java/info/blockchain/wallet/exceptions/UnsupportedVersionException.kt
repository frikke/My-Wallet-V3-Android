package info.blockchain.wallet.exceptions

class UnsupportedVersionException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
    constructor(message: String) : this(message, null)
}
