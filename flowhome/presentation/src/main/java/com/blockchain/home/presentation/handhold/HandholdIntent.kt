package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface HandholdIntent : Intent<HandholdModelState> {
    object LoadData : HandholdIntent
}
