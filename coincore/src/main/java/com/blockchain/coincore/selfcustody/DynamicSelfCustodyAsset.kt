package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class DynamicSelfCustodyAsset(
    override val currency: AssetInfo,
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
                    DynamicNonCustodialAddress(
                        address = address,
                        asset = currency as AssetInfo,
                        isDomain = isDomainAddress
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()
}

internal class DynamicNonCustodialAddress(
    override val address: String,
    override val asset: AssetInfo,
    override val label: String = address,
    override val isDomain: Boolean = false
) : CryptoAddress
