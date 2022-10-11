package com.blockchain.home.presentation.dashboard

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.allassets.SectionSize

sealed interface HomeIntent : Intent<HomeModelState> {
    data class LoadHomeAccounts(val sectionSize: SectionSize) : HomeIntent
    
    data class FilterSearch(val term: String) : HomeIntent {
        override fun isValidFor(modelState: HomeModelState): Boolean {
            return modelState.accounts is DataResource.Data
        }
    }
}
