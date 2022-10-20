package com.blockchain.home.presentation.activity

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize

data class ActivityModelState(
    val activity: DataResource<Map<TransactionGroup, List<TransactionState>>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState
