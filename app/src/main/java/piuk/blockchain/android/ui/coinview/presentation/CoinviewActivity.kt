package piuk.blockchain.android.ui.coinview.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.presentation.composable.Coinview
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.AccountExplainerBottomSheet

class CoinviewActivity :
    MVIActivity<CoinviewViewState>(),
    KoinScopeComponent,
    NavigationRouter<CoinviewNavigationEvent>,
    AccountExplainerBottomSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val scope: Scope = payloadScope
    private val viewModel: CoinviewViewModel by viewModel()

    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    val args: CoinviewArgs by lazy {
        intent.getParcelableExtra<CoinviewArgs>(CoinviewArgs.ARGS_KEY)
            ?: error("missing CoinviewArgs")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindViewModel(
            viewModel = viewModel,
            navigator = this,
            args = args
        )

        setContent {
            Coinview(
                viewModel = viewModel,
                backOnClick = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(CoinviewIntents.LoadAllData)
    }

    override fun onStateUpdated(state: CoinviewViewState) {
    }

    override fun route(navigationEvent: CoinviewNavigationEvent) {
        when (navigationEvent) {
            is CoinviewNavigationEvent.ShowAccountExplainer -> {
                navigateToAccountExplainer(
                    cvAccount = navigationEvent.cvAccount,
                    networkTicker = navigationEvent.networkTicker,
                    interestRate = navigationEvent.interestRate,
                    actions = navigationEvent.actions
                )
            }

            is CoinviewNavigationEvent.NavigateToBuy -> {
                startActivity(
                    SimpleBuyActivity.newIntent(
                        context = this,
                        asset = navigationEvent.asset.currency
                    )
                )
            }
        }
    }

    private fun navigateToAccountExplainer(
        cvAccount: CoinviewAccount,
        networkTicker: String,
        interestRate: Double,
        actions: List<StateAwareAction>
    ) {
        showBottomSheet(
            AccountExplainerBottomSheet.newInstance(
                selectedAccount = cvAccount.account,
                networkTicker = networkTicker,
                interestRate = interestRate,
                stateAwareActions = actions.toTypedArray()
            )
        )
    }

    //    private fun navigateToAccountActions(
    //        actions: List<StateAwareAction>
    //    ) {
    //        showBottomSheet(
    //            AccountExplainerBottomSheet.newInstance(
    //                selectedAccount = cvAccount.account,
    //                networkTicker = networkTicker,
    //                interestRate = interestRate,
    //                stateAwareActions = actions.toTypedArray()
    //            )
    //        )
    //    }

    // host calls
    override fun navigateToActionSheet(actions: Array<StateAwareAction>) {
        //        model.process(CoinViewIntent.UpdateViewState(CoinViewViewState.ShowAccountActionSheet(actions)))
    }

    //
    companion object {
        fun newIntent(context: Context, asset: AssetInfo): Intent {
            return Intent(context, CoinviewActivity::class.java).apply {
                putExtra(
                    CoinviewArgs.ARGS_KEY,
                    CoinviewArgs(networkTicker = asset.networkTicker)
                )
            }
        }
    }
}
