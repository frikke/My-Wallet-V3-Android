package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.ModelAccount
import com.blockchain.home.presentation.SectionSize
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.FiatCurrency

data class AssetsModelState(
    val accounts: DataResource<List<ModelAccount>> = DataResource.Loading,
    val fundsLocks: DataResource<FundsLocks?> = DataResource.Loading,
    val walletMode: WalletMode,
    private val _accountsForMode: MutableMap<WalletMode, DataResource<List<ModelAccount>>> = mutableMapOf(),
    val sectionSize: SectionSize = SectionSize.All,
    val userFiat: FiatCurrency,
    val filters: List<AssetFilter> = listOf(),
    val lastFreshDataTime: Long = 0
) : ModelState {
    init {
        _accountsForMode[walletMode] = accounts
    }

    fun accountsForMode(walletMode: WalletMode): DataResource<List<ModelAccount>> =
        _accountsForMode[walletMode] ?: DataResource.Loading
}
