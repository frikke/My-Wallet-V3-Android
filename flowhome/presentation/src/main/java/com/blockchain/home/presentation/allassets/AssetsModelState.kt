package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.ModelAccount
import com.blockchain.home.presentation.SectionSize

data class AssetsModelState(
    val accounts: DataResource<List<ModelAccount>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filters: List<AssetFilter> = listOf()
) : ModelState
