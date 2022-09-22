package com.blockchain.nfts.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
data class NftDetailNavArg(
    val nftId: String
) : ModelConfigArgs.ParcelableArgs