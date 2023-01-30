package com.blockchain.chrome

import androidx.compose.runtime.Stable
import com.blockchain.walletmode.WalletMode

@Stable
sealed interface ChromeModeOptions {
    data class MultiSelection(val modes: List<WalletMode>) : ChromeModeOptions
    data class SingleSelection(val mode: WalletMode) : ChromeModeOptions
}
