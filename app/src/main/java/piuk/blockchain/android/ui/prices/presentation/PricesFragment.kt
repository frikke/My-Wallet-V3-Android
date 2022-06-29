package piuk.blockchain.android.ui.prices.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import info.blockchain.balance.AssetInfo
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.databinding.FragmentPricesBinding

class PricesFragment :
    MVIFragment<PricesViewState>(),
    AndroidScopeComponent {

    override val scope: Scope = payloadScope
    private val viewModel: PricesViewModel by viewModel()

    private val navigationRouter: NavigationRouter<PricesNavigationEvent> by lazy {
        activity as? NavigationRouter<PricesNavigationEvent>
            ?: error("host does not implement NavigationRouter<PricesNavigationEvent>")
    }

    private var _binding: FragmentPricesBinding? = null
    private val binding: FragmentPricesBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
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
        val state = viewModel.viewState.collectAsState()

        PricesScreen(
            viewState = state.value,
            loadAssetsAvailable = ::loadAssetsAvailable,
            pricesItemClicked = ::pricesItemClicked,
            filterData = ::filterData
        )
    }

    override fun onStateUpdated(state: PricesViewState) {}

    private fun loadAssetsAvailable() {
        viewModel.onIntent(PricesIntents.LoadAssetsAvailable)
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
