package com.blockchain.preferences

interface DexPrefs {
    val dexIntroShown: Boolean
    fun markDexIntroAsSeen()
}
