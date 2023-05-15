package piuk.blockchain.android.simplebuy.upsell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.simplebuy.UpSellAnotherAssetDismissed
import piuk.blockchain.android.simplebuy.UpSellAnotherAssetMaybeLaterClicked
import piuk.blockchain.android.simplebuy.UpSellAnotherAssetMostPopularClicked
import piuk.blockchain.android.simplebuy.UpSellAnotherAssetViewed
import piuk.blockchain.android.simplebuy.upsell.viewmodel.UpSellAnotherAssetArgs
import piuk.blockchain.android.simplebuy.upsell.viewmodel.UpSellAnotherAssetIntent
import piuk.blockchain.android.simplebuy.upsell.viewmodel.UpSellAnotherAssetViewModel
import piuk.blockchain.android.simplebuy.upsell.viewmodel.UpsellAnotherAssetNavigationEvent
import piuk.blockchain.android.simplebuy.upsell.viewmodel.UpsellAnotherAssetViewState

class UpsellAnotherAssetBottomSheet :
    MVIBottomSheet<UpsellAnotherAssetViewState>(),
    KoinScopeComponent,
    NavigationRouter<UpsellAnotherAssetNavigationEvent> {

    interface Host : MVIBottomSheet.Host {
        fun launchBuyForAsset(networkTicker: String)
        fun launchBuy()
        fun onCloseUpsellAnotherAsset()
    }

    override val host: UpsellAnotherAssetBottomSheet.Host by lazy {
        super.host as? UpsellAnotherAssetBottomSheet.Host ?: throw IllegalStateException(
            "Host fragment is not a SimpleBuyUpsellAnotherAsset.Host"
        )
    }

    override val scope: Scope
        get() = payloadScope

    private val assetJustBoughtTicker: String by lazy {
        arguments?.getString(ASSET_TICKER) ?: throw IllegalStateException("No asset ticker passed")
    }

    private val viewModel by viewModel<UpSellAnotherAssetViewModel>()

    private val analytics: Analytics by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        analytics.logEvent(UpSellAnotherAssetViewed)

        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(
                    viewModel = viewModel,
                    navigator = this@UpsellAnotherAssetBottomSheet,
                    args = UpSellAnotherAssetArgs(assetJustBoughtTicker)
                )

                UpsellAnotherAssetScreen(
                    viewModel = viewModel,
                    analytics = analytics,
                    onBuyMostPopularAsset = {
                        host.launchBuyForAsset(networkTicker = it)
                        dismiss()
                    },
                    onClose = {
                        host.onCloseUpsellAnotherAsset()
                        dismiss()
                    }
                )
            }
        }
    }

    override fun onStateUpdated(state: UpsellAnotherAssetViewState) {}

    override fun route(navigationEvent: UpsellAnotherAssetNavigationEvent) {}

    companion object {

        private const val ASSET_TICKER = "ASSET_TICKER"

        fun newInstance(assetBoughtTicker: String): UpsellAnotherAssetBottomSheet =
            UpsellAnotherAssetBottomSheet()
                .apply {
                    arguments = Bundle().apply {
                        putString(ASSET_TICKER, assetBoughtTicker)
                    }
                }
    }
}

@Composable
fun UpsellAnotherAssetScreen(
    viewModel: UpSellAnotherAssetViewModel,
    analytics: Analytics,
    onBuyMostPopularAsset: (String) -> Unit,
    onClose: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: UpsellAnotherAssetViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(UpSellAnotherAssetIntent.LoadData)
        onDispose { }
    }

    viewState?.let { state ->
        Column() {
            when {
                state.isLoading -> {
                    ShimmerLoadingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTheme.dimensions.smallSpacing)
                    )
                }
                state.assetsToUpSell is DataResource.Data -> {
                    UpSellAnotherAsset(
                        assets = state.assetsToUpSell.data,
                        onBuyMostPopularAsset = { currency ->
                            analytics.logEvent(UpSellAnotherAssetMostPopularClicked(currency = currency))
                            onBuyMostPopularAsset(currency)
                        },
                        onMaybeLater = {
                            analytics.logEvent(UpSellAnotherAssetMaybeLaterClicked)
                            viewModel.onIntent(UpSellAnotherAssetIntent.DismissUpsell)
                            onClose()
                        },
                        onClose = {
                            analytics.logEvent(UpSellAnotherAssetDismissed)
                            onClose()
                        }
                    )
                }
                else -> {
                }
            }
        }
    }
}
