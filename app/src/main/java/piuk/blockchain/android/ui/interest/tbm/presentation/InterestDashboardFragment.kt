package piuk.blockchain.android.ui.interest.tbm.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.ui.interest.tbm.presentation.composables.InterestDashboardAssetItem
import piuk.blockchain.android.ui.interest.tbm.presentation.composables.InterestDashboardError
import piuk.blockchain.android.ui.interest.tbm.presentation.composables.InterestDashboardLoading
import piuk.blockchain.android.ui.interest.tbm.presentation.composables.InterestDashboardVerificationItem
import piuk.blockchain.android.ui.interest.tbm.presentation.composables.SearchField
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class InterestDashboardFragment :
    MVIFragment<InterestDashboardViewState>(),
    NavigationRouter<InterestDashboardNavigationEvent> {

    val host: piuk.blockchain.android.ui.interest.InterestDashboardFragment.InterestDashboardHost by lazy {
        activity as? piuk.blockchain.android.ui.interest.InterestDashboardFragment.InterestDashboardHost
            ?: error("Host fragment is not a InterestDashboardFragment.InterestDashboardHost")
    }

    private lateinit var composeView: ComposeView

    private val viewModel: InterestDashboardViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        bindViewModel(viewModel = viewModel, navigator = this, args = ModelConfigArgs.NoArgs)

        loadData()
    }

    private fun setupViews() {
        composeView.apply {
            setContent {
                ScreenContent()
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
                InterestDashboardError(::loadData)
            }

            state.value.isLoading.not() && state.value.isError.not() -> {

                Column {
                    Box {
                        SearchField {
                            viewModel.onIntent(InterestDashboardIntents.FilterData(it))
                        }
                    }

                    LazyColumn {
                        items(
                            items = state.value.data,
                            itemContent = {
                                when (it) {
                                    is InterestDashboardItem.InterestAssetInfoItem -> {
                                        InterestDashboardAssetItem(
                                            assetInfo = it.assetInterestInfo.assetInfo,
                                            assetInterestDetail = it.assetInterestInfo.assetInterestDetail,
                                            isKycGold = state.value.isKycGold,
                                            interestItemClicked = ::interestItemClicked
                                        )
                                    }

                                    InterestDashboardItem.InterestIdentityVerificationItem -> {
                                        InterestDashboardVerificationItem(::startKyc)
                                    }
                                }
                            }
                        )
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
                host.showInterestSummarySheet(navigationEvent.account)
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

    private fun loadData() {
        viewModel.onIntent(InterestDashboardIntents.LoadData)
    }

    private fun startKyc() {
        host.startKyc()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }
}
