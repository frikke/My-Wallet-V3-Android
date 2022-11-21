package com.blockchain.home.presentation.activity.detail.custodial

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface CustodialActivityDetailIntent : Intent<CustodialActivityDetailModelState> {
    object LoadActivityDetail : CustodialActivityDetailIntent
}
