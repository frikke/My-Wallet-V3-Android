package info.blockchain.wallet.exceptions

class DecryptionException(message: String? = null, cause: Throwable? = null) :
    Exception(message ?: cause?.message, cause) {
    constructor(cause: Throwable) : this(null, cause)
    constructor(message: String) : this(message, null)
}
