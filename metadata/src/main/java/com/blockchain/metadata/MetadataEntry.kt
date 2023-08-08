package com.blockchain.metadata

/**
 * Adding the legacy/unused ones for feature reference
 */
enum class MetadataEntry(val index: Int) {

    @Suppress("UNUSED_PARAMETER")
    WHATS_NEW(2),

    @Suppress("UNUSED_PARAMETER")
    BUY_SELL(3),

    @Suppress("UNUSED_PARAMETER")
    CONTACTS(4),

    @Suppress("UNUSED_PARAMETER")
    SHAPE_SHIFT(6),

    @Suppress("UNUSED_PARAMETER")
    LOCKBOX(9),
    METADATA_ETH(5),
    METADATA_BCH(7),
    NABU_LEGACY_CREDENTIALS(10),
    METADATA_XLM(11),
    WALLET_CREDENTIALS(12),
    WALLET_CONNECT_METADATA(13),
    BLOCKCHAIN_UNIFIED_CREDENTIALS(14)
}
