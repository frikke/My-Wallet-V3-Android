package com.blockchain.home.presentation.fiat.fundsdetail

import androidx.annotation.StringRes
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class FiatFundsDetailViewState(
    val detail: DataResource<FiatFundsDetail>,
    val data: DataResource<FiatFundsDetailData>,
    val showWithdrawChecksLoading: Boolean,
    val actionError: FiatActionErrorState?
) : ViewState

data class FiatFundsDetail(
    val account: FiatAccount,
    val name: String,
    val logo: String
)

data class FiatActionErrorState(@StringRes val message: Int)