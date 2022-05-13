package com.blockchain.metadata

import org.junit.Assert
import org.junit.Test

class MetadataEntryTest {
    @Test
    fun `metadata entries index should be unique`() {
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 13 } == listOf(MetadataEntry.WALLET_CONNECT_METADATA)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 5 } == listOf(MetadataEntry.METADATA_ETH)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 7 } == listOf(MetadataEntry.METADATA_BCH)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 10 } == listOf(MetadataEntry.NABU_LEGACY_CREDENTIALS)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 14 } == listOf(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 12 } == listOf(MetadataEntry.WALLET_CREDENTIALS)
        )
        Assert.assertTrue(
            MetadataEntry.values().filter { it.index == 11 } == listOf(MetadataEntry.METADATA_XLM)
        )
    }
}
