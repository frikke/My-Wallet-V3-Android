package com.blockchain.nfts.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.detail.screen.NftDetail
import com.blockchain.nfts.help.screen.NftHelpScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.nftGraph(
    openExternalUrl: (url: String) -> Unit,
    onBackPressed: () -> Unit
) {
    bottomSheet(navigationEvent = NftDestination.Help) {
        ChromeBottomSheet(onClose = onBackPressed) {
            NftHelpScreen(
                onBuyClick = {
                    openExternalUrl(OPENSEA_URL)
                }
            )
        }
    }

    bottomSheet(navigationEvent = NftDestination.Detail) { backStackEntry ->
        val nftId = backStackEntry.arguments?.getComposeArgument(ARG_NFT_ID).orEmpty()
        val address = backStackEntry.arguments?.getComposeArgument(ARG_ADDRESS).orEmpty()
        val pageKey = backStackEntry.arguments?.getComposeArgument(ARG_PAGE_KEY)

        ChromeBottomSheet(onClose = onBackPressed) {
            NftDetail(
                nftId = nftId,
                address = address,
                pageKey = pageKey
            )
        }
    }
}
