package com.blockchain.wallet

interface BackupWallet {
    /**
     * Returns an ordered list of [Int], [String] pairs which can be used to confirm mnemonic.
     */
    fun getConfirmSequence(secondPassword: String?): List<Pair<Int, String>>

    /**
     * Returns a [MutableList] of Strings representing the user's backup mnemonic, or null
     * if the mnemonic isn't found.
     */
    fun getMnemonic(secondPassword: String?): List<String>?
}
