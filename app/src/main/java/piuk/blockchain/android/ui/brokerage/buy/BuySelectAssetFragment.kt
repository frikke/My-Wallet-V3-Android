package piuk.blockchain.android.ui.brokerage.buy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.home.presentation.navigation.HomeLaunch
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.openUrl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuyCryptoFragment
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.brokerage.buy.composable.BuySelectAsset
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8

class BuySelectAssetFragment : Fragment(), KoinScopeComponent {

    override val scope: Scope = payloadScope
    private val viewModel: BuySelectAssetViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        handleNavigation()

        return ComposeView(requireContext()).apply {
            setContent {
                BuySelectAsset(
                    viewModel = viewModel,
                    onErrorContactSupportClicked = {
                        startActivity(SupportCentreActivity.newIntent(requireContext()))
                    },
                    onEmptyStateClicked = { reason ->
                        when (reason) {
                            is BlockedReason.Sanctions.RussiaEU5 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5)
                            is BlockedReason.Sanctions.RussiaEU8 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU8)
                            is BlockedReason.NotEligible -> startActivity(
                                SupportCentreActivity.newIntent(requireContext())
                            )
                            else -> {
                                // n/a
                            }
                        }
                    },
                    startKycClicked = {
                        KycNavHostActivity.startForResult(
                            this@BuySelectAssetFragment, CampaignType.SimpleBuy,
                            HomeLaunch.KYC_STARTED
                        )
                    }
                )
            }
        }
    }

    private fun handleNavigation() {
        lifecycleScope.launch {
            viewModel.navigationEventFlow.flowWithLifecycle(lifecycle = lifecycle)
                .collectLatest {
                    when (it) {
                        is BuySelectAssetNavigation.PendingOrders -> {
                            showPendingOrdersBottomSheet(it.maxTransactions)
                        }
                        is BuySelectAssetNavigation.SimpleBuy -> {
                            startActivity(
                                SimpleBuyActivity.newIntent(
                                    context = activity as Context,
                                    asset = it.assetInfo,
                                    launchFromNavigationBar = true,
                                    launchKycResume = false,
                                    fromRecurringBuy = arguments?.getBoolean(ARG_FROM_RECURRING_BUY) ?: false
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun showPendingOrdersBottomSheet(maxTransactions: Int) {
        BuyPendingOrdersBottomSheet.newInstance(maxTransactions).show(
            childFragmentManager,
            BuyPendingOrdersBottomSheet.TAG
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HomeLaunch.KYC_STARTED) {
            viewModel.onIntent(BuySelectAssetIntent.LoadEligibility)
        }
    }

    companion object {
        private const val ARG_FROM_RECURRING_BUY = "ARG_FROM_RECURRING_BUY"

        fun newInstance(
            fromRecurringBuy: Boolean = false
        ) = BuySelectAssetFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_FROM_RECURRING_BUY, fromRecurringBuy)
            }
        }
    }
}
