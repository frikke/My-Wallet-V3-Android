package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage

data class ActivityModelState(
    val activityPage: DataResource<UnifiedActivityPage> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState
