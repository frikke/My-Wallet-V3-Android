package piuk.blockchain.android.ui.interest.tbm.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentInterestDashboardBinding
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardAdapter
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardItem

class InterestDashboardFragment : MVIFragment<InterestDashboardViewState>(), NavigationRouter<NavigationEvent> {

    val host: InterestDashboardHost by lazy {
        activity as? InterestDashboardHost
            ?: error("Host fragment is not a InterestDashboardFragment.InterestDashboardHost")
    }

    private lateinit var composeView: ComposeView

    private var _binding: FragmentInterestDashboardBinding? = null
    private val binding: FragmentInterestDashboardBinding
        get() = _binding!!

    private val viewModel: InterestDashboardViewModel by viewModel()

    private val listAdapter: InterestDashboardAdapter by lazy {
        InterestDashboardAdapter(
            verificationClicked = ::startKyc,
            itemClicked = ::interestItemClicked
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterestDashboardBinding.inflate(inflater, container, false)

        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.interestDashboardList.adapter = listAdapter

        setupViews()
        bindViewModel(viewModel = viewModel, navigator = this, args = ModelConfigArgs.NoArgs)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressBar()
                }
            }

            state.value.isError -> {
            }

            state.value.isLoading.not() && state.value.isError.not() -> {
                LazyColumn {
                    items(
                        items = state.value.data,
                        itemContent = {
                            when (it) {
                                is InterestDashboardItem.InterestAssetInfoItem -> {
                                    AssetInterestItem(
                                        assetInfo = it.assetInterestInfo.assetInfo,
                                        assetInterestDetail = it.assetInterestInfo.assetInterestDetail,
                                        isKycGold = state.value.isKycGold
                                    )
                                }

                                InterestDashboardItem.InterestIdentityVerificationItem -> {
                                    InterestDashboardVerificationItem {}
                                }
                            }
                        }
                    )
                }
            }

        }
    }

    override fun onStateUpdated(state: InterestDashboardViewState) {
        with(binding) {
            interestDashboardProgress.visibleIf { state.isLoading }

            interestDashboardList.visibleIf { state.isLoading.not() && state.isError.not() }

            interestError.setDetails(
                title = R.string.rewards_error_title,
                description = R.string.rewards_error_desc,
                contactSupportEnabled = true
            ) {
                //                loadInterestDetails()
            }
            interestError.visibleIf { state.isError }

            println("------ data: ${state.data}")

            listAdapter.items = state.data
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun route(navigationEvent: NavigationEvent) {
    }

    private fun interestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        println("------ cryptoCurrency: $cryptoCurrency")
        //        compositeDisposable += coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).subscribe {
        //            val interestAccount = it.accounts.first() as CryptoInterestAccount
        //            if (hasBalance) {
        //                host.showInterestSummarySheet(interestAccount)
        //            } else {
        //                startActivity(
        //                    TransactionFlowActivity.newIntent(
        //                        context = requireContext(),
        //                        target = it.accounts.first(),
        //                        action = AssetAction.InterestDeposit
        //                    )
        //                )
        //            }
        //        }
    }

    private fun startKyc() {
        host.startKyc()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }
}
