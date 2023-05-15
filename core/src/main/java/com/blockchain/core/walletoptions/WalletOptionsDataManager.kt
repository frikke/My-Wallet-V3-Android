package com.blockchain.core.walletoptions

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.store.asSingle
import com.blockchain.sunriver.XlmHorizonUrlFetcher
import com.blockchain.sunriver.XlmTransactionTimeoutFetcher
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.api.data.XLM_DEFAULT_TIMEOUT_SECS
import io.reactivex.rxjava3.core.Single

class WalletOptionsDataManager(
    private val walletOptionsStore: WalletOptionsStore,
) : XlmTransactionTimeoutFetcher, XlmHorizonUrlFetcher {

    private val walletOptionsSingle: Single<WalletOptions>
        get() = walletOptionsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).asSingle()

    override fun xlmHorizonUrl(def: String): Single<String> =
        walletOptionsSingle
            .map { it.stellarHorizonUrl }
            .onErrorReturn { def }

    override fun transactionTimeout(): Single<Long> =
        walletOptionsSingle
            .map { it.xlmTransactionTimeout }
            .onErrorReturn { XLM_DEFAULT_TIMEOUT_SECS }

    fun isXlmAddressExchange(address: String): Single<Boolean> = walletOptionsSingle.map {
        address.uppercase() in it.xmlExchangeAddresses.map { address -> address.uppercase() }
    }
}
