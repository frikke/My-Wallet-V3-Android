package com.blockchain.home.presentation.activity.custodial.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.list.ActivityModelState

sealed interface CustodialActivityIntent : Intent<CustodialActivityModelState> {
    data class LoadActivity(val sectionSize: SectionSize) : CustodialActivityIntent

    data class FilterSearch(val term: String) : CustodialActivityIntent {
        override fun isValidFor(modelState: CustodialActivityModelState): Boolean {
            return modelState.activity is DataResource.Data
        }
    }
}
