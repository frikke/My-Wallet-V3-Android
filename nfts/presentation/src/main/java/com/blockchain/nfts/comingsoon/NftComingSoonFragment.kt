package com.blockchain.nfts.comingsoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blockchain.nfts.comingsoon.screen.NftComingSoonScreen
import com.blockchain.nfts.domain.service.NftService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class NftComingSoonFragment : Fragment() {

    private val nftService: NftService by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        CoroutineScope(Dispatchers.IO).launch {
            val data = nftService.getNftForAddress(address = "0x6E33d3F19172357d61FA6C7266fa7766be24b210")

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
