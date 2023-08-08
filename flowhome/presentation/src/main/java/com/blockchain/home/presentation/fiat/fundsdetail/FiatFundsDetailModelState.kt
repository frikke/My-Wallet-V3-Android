package com.blockchain.home.presentation.fiat.fundsdetail

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import info.blockchain.balance.Money

data class FiatFundsDetailModelState(
    val account: DataResource<FiatAccount> = DataResource.Loading,
    val data: DataResource<FiatFundsDetailData> = DataResource.Loading,
    val withdrawChecksLoading: Boolean = false,
    val actionError: FiatActionError = FiatActionError.None
) : ModelState

data class FiatFundsDetailData(
    val balance: Money,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean
)

sealed interface FiatActionError {
    object WithdrawalInProgress : FiatActionError
    object Unknown : FiatActionError
    object None : FiatActionError
}
