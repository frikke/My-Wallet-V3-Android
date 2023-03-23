package com.blockchain.preferences

interface DexPrefs {
    val dexIntroShown: Boolean
    fun markDexIntroAsSeen()
    var selectedDestinationCurrencyTicker: String
    var selectedSlippageIndex: Int
}
