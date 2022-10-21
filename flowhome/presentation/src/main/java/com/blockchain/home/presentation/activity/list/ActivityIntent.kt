package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.SectionSize

sealed interface ActivityIntent : Intent<ActivityModelState> {
    data class LoadActivity(val sectionSize: SectionSize) : ActivityIntent
}
