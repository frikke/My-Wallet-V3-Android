package com.blockchain.coincore.wrap

import info.blockchain.wallet.util.FormatsUtil

// Wrapper over the 'static' FormatsUtils object, to facilitate testing
// The FormatsUtils object needs to turned into an injectable class, but it's
// hooked into many static contexts in the lower level code, so this is a
// reasonably big task. TODO

class FormatUtilities {
    fun isValidEthereumAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)

    companion object {
        const val BTC_PREFIX = FormatsUtil.BTC_PREFIX
        const val BCH_PREFIX = FormatsUtil.BCH_PREFIX
        const val ETHEREUM_PREFIX = FormatsUtil.ETHEREUM_PREFIX
    }
}