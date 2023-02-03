package com.blockchain.nfts.collection

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Activity
import com.blockchain.componentlib.icons.Grid
import com.blockchain.componentlib.icons.Icons
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftCollectionViewState(
    val isPullToRefreshLoading: Boolean,
    val showNextPageLoading: Boolean,
    val collection: DataResource<List<NftAsset>>,
    val displayType: DisplayType
) : ViewState

sealed class DisplayType(
    val columnCount: Int,
    val icon: ImageResource.Local
) {
    object Grid : DisplayType(
        columnCount = 2,
        icon = Icons.Grid
    )

    object List : DisplayType(
        columnCount = 1,
        icon = Icons.Activity
    )

    override fun equals(other: Any?): Boolean {
        return other?.let { other::class.java == this::class.java } ?: false
    }

    override fun hashCode(): Int = columnCount
}
