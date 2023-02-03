package com.blockchain.nfts.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.help.screen.NftHelpScreen
import com.blockchain.nfts.navigation.NftDestination
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.nftGraph(
    openExternalUrl: (url: String) -> Unit
) {
    bottomSheet(navigationEvent = NftDestination.Help) {
        ChromeBottomSheet {
            NftHelpScreen(
                onBuyClick = {
                    openExternalUrl(OPENSEA_URL)
                }
            )
        }
    }
}
