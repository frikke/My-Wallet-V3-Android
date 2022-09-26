package com.blockchain.nfts.comingsoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.nfts.NftAnalyticsEvents
import com.blockchain.nfts.comingsoon.screen.NftComingSoonScreen
import org.koin.java.KoinJavaComponent

class NftComingSoonFragment :
    Fragment(),
    Analytics by KoinJavaComponent.get(Analytics::class.java) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logEvent(NftAnalyticsEvents.ScreenViewed)

        return ComposeView(requireContext()).apply {
            setContent {
                NftComingSoonScreen()
            }
        }
    }
}













