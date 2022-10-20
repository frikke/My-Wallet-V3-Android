package com.blockchain.home.presentation.activity

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.allassets.AssetsModelState
import com.blockchain.home.presentation.SectionSize

sealed interface ActivityIntent : Intent<ActivityModelState> {
    data class LoadActivity(val sectionSize: SectionSize) : ActivityIntent

}
