package info.blockchain.wallet.metadata

object MetadataUrls {

    // Base endpoint for Contacts/Shared Metadata/CryptoMatrix
    private const val IWCS = "iwcs"

    // Base endpoint for generic Metadata
    const val METADATA = "metadata"

    // Complete paths
    const val AUTH = "$IWCS/auth"
    const val TRUSTED = "$IWCS/trusted"
    const val MESSAGE = "$IWCS/message"
    const val MESSAGES = "$IWCS/messages"
    const val SHARE = "$IWCS/share"
}
