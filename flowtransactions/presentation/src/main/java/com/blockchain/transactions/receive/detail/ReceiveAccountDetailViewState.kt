package com.blockchain.transactions.receive.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.image.LogoValue
import com.blockchain.presentation.analytics.TxFlowAnalyticsAccountType
import info.blockchain.balance.CoinNetwork

data class ReceiveAccountDetailViewState(
    val assetTicker: String,
    val accountLabel: String,
    private val mainLogo: String,
    private val tagLogo: String?,
    private val coinNetwork: CoinNetwork?,
    val receiveAddress: DataResource<ReceiveAddressViewState>,
    // for analytics
    val accountType: TxFlowAnalyticsAccountType,
    val networkTicker: String
) : ViewState {
    val isCopyButtonEnabled: Boolean
        get() = receiveAddress is DataResource.Data

    val icon: LogoValue
        get() = tagLogo?.let {
            LogoValue.SmallTag(main = mainLogo, tag = tagLogo)
        } ?: LogoValue.SingleIcon(url = mainLogo)

    val nativeAsset: Pair<String, String>?
        get() = coinNetwork?.let {
            check(tagLogo != null)
            Pair(it.shortName, tagLogo)
        }
}

data class ReceiveAddressViewState(
    val uri: String,
    val address: String,
    val memo: String?,
    val isRotating: Boolean
)
