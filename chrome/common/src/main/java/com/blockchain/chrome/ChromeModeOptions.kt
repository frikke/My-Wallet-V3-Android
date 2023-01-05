package com.blockchain.chrome

import com.blockchain.walletmode.WalletMode

sealed interface ChromeModeOptions {
    data class MultiSelection(val modes: List<WalletMode>) : ChromeModeOptions
    data class SingleSelection(val mode: WalletMode) : ChromeModeOptions
}
