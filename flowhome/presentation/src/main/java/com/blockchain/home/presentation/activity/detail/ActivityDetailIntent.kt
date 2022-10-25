package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ActivityDetailIntent : Intent<ActivityDetailModelState> {
    object LoadActivityDetail : ActivityDetailIntent
}
