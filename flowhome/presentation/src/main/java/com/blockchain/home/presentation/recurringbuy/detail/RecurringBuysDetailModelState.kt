package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.ModelAccount
import com.blockchain.home.presentation.SectionSize
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.FiatCurrency

data class RecurringBuysDetailModelState(
    val recurringBuy: DataResource<RecurringBuy> = DataResource.Loading,
    val cancelationInProgress: Boolean = false
) : ModelState
