package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.list.ActivityModelState

sealed interface ActivityDetailIntent : Intent<ActivityDetailModelState> {
    object LoadActivityDetail : ActivityDetailIntent
}
