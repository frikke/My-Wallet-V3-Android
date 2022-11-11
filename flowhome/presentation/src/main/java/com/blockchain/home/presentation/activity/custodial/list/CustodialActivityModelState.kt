package com.blockchain.home.presentation.activity.custodial.list

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage

data class CustodialActivityModelState(
    val activity: DataResource<List<ActivitySummaryItem>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState
