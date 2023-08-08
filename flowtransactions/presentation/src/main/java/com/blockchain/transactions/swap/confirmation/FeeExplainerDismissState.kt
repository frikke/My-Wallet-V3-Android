package com.blockchain.transactions.swap.confirmation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FeeExplainerDismissState {
    var isDismissed: Boolean by mutableStateOf(false)
}
