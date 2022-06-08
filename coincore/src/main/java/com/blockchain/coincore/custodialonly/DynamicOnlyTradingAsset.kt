package com.blockchain.coincore.custodialonly

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class DynamicOnlyTradingAsset(
    override val assetInfo: AssetInfo,
    private val addressValidation: String? = null
) : CryptoAssetBase() {

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(emptyList())

    private val addressRegex: Regex? by unsafeLazy {
        addressValidation?.toRegex()
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        addressRegex?.let {
            if (address.matches(it)) {
                Maybe.just(
                    DynamicCustodialAddress(
                        address = address,
                        asset = assetInfo as AssetInfo,
                        isDomain = isDomainAddress
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()
}

internal class DynamicCustodialAddress(
    override val address: String,
    override val asset: AssetInfo,
    override val label: String = address,
    override val isDomain: Boolean = false
) : CryptoAddress
