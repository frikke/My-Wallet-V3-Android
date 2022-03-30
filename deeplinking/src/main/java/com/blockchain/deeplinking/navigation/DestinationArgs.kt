package com.blockchain.deeplinking.navigation

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.MaybeSubject

class DestinationArgs(
    private val assetCatalogue: AssetCatalogue,
    private val coincore: Coincore
) {

    fun getAssetInfo(networkTicker: String): AssetInfo? =
        assetCatalogue.assetInfoFromNetworkTicker(networkTicker)

    fun getSendSourceCryptoAccount(assetInfo: AssetInfo, address: String): Maybe<CryptoAccount> {
        val subject = MaybeSubject.create<CryptoAccount>()
        coincore.findAccountByAddress(assetInfo, address).subscribeBy(
            onSuccess = { account ->
                if (account is CryptoAccount) {
                    subject.onSuccess(account)
                } else {
                    subject.onError(
                        Exception("Unable to start Send from deeplink. Account is not a CryptoAccount")
                    )
                }
            },
            onComplete = {
                subject.onError(
                    Exception("Unable to start Send from deeplink. Account not found")
                )
            },
            onError = {
                subject.onError(it)
            }
        )

        return subject
    }
}
