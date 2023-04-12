package com.blockchain.domain.wiretransfer

data class WireTransferDetails(
    val sections: List<WireTransferDetailsSection>,
    val footers: List<WireTransferDetailsFooter>,
)

data class WireTransferDetailsSection(
    val name: String,
    val entries: List<WireTransferDetailsSectionEntry>,
)

data class WireTransferDetailsSectionEntry(
    val title: String,
    val message: String,
    val isImportant: Boolean,
    val help: String?,
)

data class WireTransferDetailsFooter(
    val title: String,
    val message: String,
    val icon: String?,
    val isImportant: Boolean,
)
