package com.blockchain.addressverification.domain.model

data class AutocompleteAddress(
    val id: String,
    val type: AutocompleteAddressType,
    val title: String,
    val titleHighlightRanges: List<IntRange>,
    val description: String,
    val descriptionHighlightRanges: List<IntRange>,
    val containedAddressesCount: Int?
)

enum class AutocompleteAddressType {
    ADDRESS,
    STREET,
    OTHER
}
