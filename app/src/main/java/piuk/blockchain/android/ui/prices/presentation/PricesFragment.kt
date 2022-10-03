package piuk.blockchain.android.ui.prices.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class PricesFragment :
    MVIFragment<PricesViewState>(),
    AndroidScopeComponent {

    override val scope: Scope = payloadScope
    private val viewModel: PricesViewModel by viewModel()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val navigationRouter: NavigationRouter<PricesNavigationEvent> by lazy {
        activity as? NavigationRouter<PricesNavigationEvent>
            ?: error("host does not implement NavigationRouter<PricesNavigationEvent>")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    ScreenContent()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadAssetsAvailable()
    }

    private fun setupViewModel() {
        bindViewModel(
            viewModel = viewModel,
            navigator = navigationRouter,
            args = ModelConfigArgs.NoArgs
        )
    }

    @Composable
    private fun ScreenContent() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
            viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
        }
        val viewState: PricesViewState? by stateFlowLifecycleAware.collectAsState(null)
        viewState?.let {
            PricesScreen(
                viewState = it,
                retryAction = ::loadAssetsAvailable,
                pricesItemClicked = ::pricesItemClicked,
                filterData = ::filterData
            )
        }
    }

    override fun onStateUpdated(state: PricesViewState) {}

    private fun loadAssetsAvailable() {
        viewModel.onIntent(PricesIntents.LoadAssetsAvailable(currencyPrefs.selectedFiatCurrency))
    }

    private fun pricesItemClicked(cryptoCurrency: AssetInfo) {
        viewModel.onIntent(
            PricesIntents.PricesItemClicked(cryptoCurrency = cryptoCurrency)
        )
    }

    private fun filterData(filter: String) {
        viewModel.onIntent(PricesIntents.FilterData(filter))
    }

    companion object {
        fun newInstance(): PricesFragment = PricesFragment()
    }
}
