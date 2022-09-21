package com.blockchain.nfts.comingsoon

import android.os.Bundle
import android.util.StatsLog.logEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.NftAnalyticsEvents
import com.blockchain.nfts.comingsoon.screen.NftComingSoonScreen
import org.koin.java.KoinJavaComponent
import com.blockchain.nfts.domain.service.NftService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class NftComingSoonFragment :
    Fragment(),
    Analytics by KoinJavaComponent.get(Analytics::class.java),
KoinScopeComponent{

    override val scope: Scope = payloadScope
    private val nftService: NftService by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logEvent(NftAnalyticsEvents.ScreenViewed)

        CoroutineScope(Dispatchers.IO).launch {
            val data = nftService.getNftForAddress(address = "0x5D70101143BF7bbc889D757613e2B2761bD447EC")

            withContext(Dispatchers.Main) {

            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                NftComingSoonScreen()
            }
        }
    }
}
//
//package com.blockchain.nfts.comingsoon
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.compose.ui.platform.ComposeView
//import androidx.fragment.app.Fragment
//import com.blockchain.analytics.Analytics
//import com.blockchain.nfts.NftAnalyticsEvents
//import com.blockchain.nfts.comingsoon.screen.NftComingSoonScreen
//import org.koin.java.KoinJavaComponent
//
//class NftComingSoonFragment :
//    Fragment(),
//    Analytics by KoinJavaComponent.get(Analytics::class.java) {
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        logEvent(NftAnalyticsEvents.ScreenViewed)
//
//        return ComposeView(requireContext()).apply {
//            setContent {
//                NftComingSoonScreen()
//            }
//        }
//    }
//}













