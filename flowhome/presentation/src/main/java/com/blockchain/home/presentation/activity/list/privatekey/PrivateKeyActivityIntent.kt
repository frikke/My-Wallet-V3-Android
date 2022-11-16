package com.blockchain.home.presentation.activity.list.privatekey

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize

sealed interface PrivateKeyActivityIntent : Intent<PrivateKeyActivityModelState> {
    data class LoadActivity(val sectionSize: SectionSize) : PrivateKeyActivityIntent

    data class FilterSearch(val term: String) : PrivateKeyActivityIntent {
        override fun isValidFor(modelState: PrivateKeyActivityModelState): Boolean {
            return modelState.activityItems is DataResource.Data
        }
    }
}
