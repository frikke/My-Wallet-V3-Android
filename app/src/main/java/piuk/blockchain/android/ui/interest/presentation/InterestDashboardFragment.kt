package piuk.blockchain.android.ui.interest.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.control.Search
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.interest.presentation.composables.InterestDashboardAssetItem
import piuk.blockchain.android.ui.interest.presentation.composables.InterestDashboardError
import piuk.blockchain.android.ui.interest.presentation.composables.InterestDashboardLoading
import piuk.blockchain.android.ui.interest.presentation.composables.InterestDashboardVerificationItem
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class InterestDashboardFragment :
    MVIFragment<InterestDashboardViewState>(),
    NavigationRouter<InterestDashboardNavigationEvent> {

    private val viewModel: InterestDashboardViewModel by viewModel()
    private val sharedViewModel: InterestDashboardSharedViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        loadDashboard()
    }

    private fun setupViewModel() {
        bindViewModel(viewModel = viewModel, navigator = this, args = ModelConfigArgs.NoArgs)

        lifecycleScope.launch {
            sharedViewModel.refreshBalancesFlow.flowWithLifecycle(lifecycle).collect {
                loadDashboard()
            }
        }
    }

    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()

        when {
            state.value.isLoading -> {
                InterestDashboardLoading()
            }

            state.value.isError -> {
                InterestDashboardError(::loadDashboard)
            }

            state.value.isLoading.not() && state.value.isError.not() -> {
                Column {
                    Box(
                        modifier = Modifier.padding(
                            start = dimensionResource(R.dimen.standard_margin),
                            end = dimensionResource(R.dimen.standard_margin)
                        )
                    ) {
                        Search(label = stringResource(R.string.search_coins_hint)) {
                            viewModel.onIntent(InterestDashboardIntents.FilterData(it))
                        }
                    }

                    LazyColumn {
                        if (state.value.isKycGold.not()) {
                            item {
                                InterestDashboardVerificationItem(::startKyc)
                            }
                        }

                        items(
                            items = state.value.data,
                        ) {
                            InterestDashboardAssetItem(
                                assetInfo = it.assetInfo,
                                assetInterestDetail = it.interestDetail,
                                isKycGold = state.value.isKycGold,
                                interestItemClicked = ::interestItemClicked
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStateUpdated(state: InterestDashboardViewState) {
    }

    override fun route(navigationEvent: InterestDashboardNavigationEvent) {
        when (navigationEvent) {
            is InterestDashboardNavigationEvent.NavigateToInterestSummarySheet -> {
                sharedViewModel.showInterestSummary(navigationEvent.account)
            }

            is InterestDashboardNavigationEvent.NavigateToTransactionFlow -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = requireContext(),
                        target = navigationEvent.account,
                        action = AssetAction.InterestDeposit
                    )
                )
            }
        }
    }

    private fun interestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        viewModel.onIntent(
            InterestDashboardIntents.InterestItemClicked(cryptoCurrency = cryptoCurrency, hasBalance = hasBalance)
        )
    }

    private fun loadDashboard() {
        viewModel.onIntent(InterestDashboardIntents.LoadDashboard)
    }

    private fun startKyc() {
        sharedViewModel.startKyc()
    }

    @Preview
    @Composable
    fun PreviewScreenContent() {
        ScreenContent()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }
}
