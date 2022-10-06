package com.blockchain.nfts.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
data class NftDetailNavArgs(
    val nftId: String,
    val address: String
) : ModelConfigArgs.ParcelableArgs {
    companion object {
        const val ARGS_KEY: String = "NftDetailNavArgs"
    }
}
