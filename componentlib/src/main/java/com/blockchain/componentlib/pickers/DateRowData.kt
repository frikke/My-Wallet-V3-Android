package com.blockchain.componentlib.pickers

data class DateRowData(
    val label: String,
    val date: String,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {}
)
