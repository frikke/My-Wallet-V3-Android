package com.blockchain.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface DexPrefs {
    val dexIntroShown: Boolean
    fun markDexIntroAsSeen()
    var selectedDestinationCurrencyTicker: String
    var selectedSlippageIndex: Int
    var selectedChainId: Int
}
