package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsModelState

sealed interface ActivityIntent : Intent<ActivityModelState> {
    data class LoadActivity(val sectionSize: SectionSize) : ActivityIntent

    data class FilterSearch(val term: String) : ActivityIntent {
        override fun isValidFor(modelState: ActivityModelState): Boolean {
            return modelState.activityPage is DataResource.Data
        }
    }
}
