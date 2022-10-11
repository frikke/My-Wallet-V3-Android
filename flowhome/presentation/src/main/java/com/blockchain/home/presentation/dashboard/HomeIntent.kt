package com.blockchain.home.presentation.dashboard

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class HomeIntent : Intent<HomeModelState> {
    object LoadHomeAccounts : HomeIntent()
}
