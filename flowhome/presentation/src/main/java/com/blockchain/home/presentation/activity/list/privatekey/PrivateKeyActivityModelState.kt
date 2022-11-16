package com.blockchain.home.presentation.activity.list.privatekey

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem

data class PrivateKeyActivityModelState(
    val activityItems: DataResource<List<UnifiedActivityItem>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState
