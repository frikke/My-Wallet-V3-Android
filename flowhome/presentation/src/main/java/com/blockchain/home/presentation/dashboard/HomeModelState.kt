package com.blockchain.home.presentation.dashboard

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.extensions.replace
import info.blockchain.balance.Money

data class HomeModelState(
    val accounts: DataResource<List<ModelAccount>>
) : ModelState

data class ModelAccount(
    val singleAccount: SingleAccount,
    val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money>,
    val exchangeRateDayDelta: DataResource<Double>
)

data class HomeActivity(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val amount: Money,
    val userFiatAmount: Money
)


