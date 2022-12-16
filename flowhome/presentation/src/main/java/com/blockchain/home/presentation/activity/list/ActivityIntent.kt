package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize

sealed interface ActivityIntent<ACTIVITY_MODEL> : Intent<ActivityModelState<ACTIVITY_MODEL>> {
    data class LoadActivity<ACTIVITY_MODEL>(val sectionSize: SectionSize) : ActivityIntent<ACTIVITY_MODEL> {
        override fun isValidFor(modelState: ActivityModelState<ACTIVITY_MODEL>): Boolean {
            return modelState.activityItems !is DataResource.Data
        }
    }

    data class FilterSearch<ACTIVITY_MODEL>(val term: String) : ActivityIntent<ACTIVITY_MODEL> {
        override fun isValidFor(modelState: ActivityModelState<ACTIVITY_MODEL>): Boolean {
            return modelState.activityItems is DataResource.Data
        }
    }
}
