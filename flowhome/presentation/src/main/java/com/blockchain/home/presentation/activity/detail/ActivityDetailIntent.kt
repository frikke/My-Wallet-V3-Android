package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ActivityDetailIntent<ACTIVITY_MODEL> : Intent<ActivityDetailModelState<ACTIVITY_MODEL>> {
    class LoadActivityDetail<ACTIVITY_MODEL> : ActivityDetailIntent<ACTIVITY_MODEL>
}
