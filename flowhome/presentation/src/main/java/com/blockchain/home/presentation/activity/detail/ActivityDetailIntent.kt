package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.activity.common.ClickAction

sealed interface ActivityDetailIntent : Intent<ActivityDetailModelState> {
    object LoadActivityDetail : ActivityDetailIntent
}
