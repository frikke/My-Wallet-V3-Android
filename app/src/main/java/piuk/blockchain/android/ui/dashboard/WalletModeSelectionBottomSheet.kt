package piuk.blockchain.android.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.R

class WalletModeSelectionBottomSheet : BottomSheetDialogFragment(), AndroidScopeComponent {
    interface Host {
        fun onChangeActiveModeRequested()
        fun onActiveModeChanged(
            walletMode: WalletMode,
        )
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a AccountActionsBottomSheet.Host")
    }

    private val viewModel: WalletModeSelectionViewModel by viewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity())
        dialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_margin))),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SheetHeader(
                            onClosePress = { dismiss() },
                            shouldShowDivider = false
                        )
                        WalletModes(viewModel)
                    }
                }
            }
        )
        return dialog
    }

    companion object {
        fun newInstance(): WalletModeSelectionBottomSheet = WalletModeSelectionBottomSheet()
    }

    @Composable
    fun WalletModes(viewModel: WalletModeSelectionViewModel) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
            viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
        }
        val viewState: WalletModeSelectionViewState? by stateFlowLifecycleAware.collectAsState(null)

        viewState?.let { state ->
            WalletModesDialogContent(
                totalBalance = state.totalBalance,
                portfolioBalanceState = state.portfolioBalance,
                defiWalletBalance = state.defiWalletBalance,
                selectedMode = state.enabledWalletMode,
                onItemClicked = {
                    viewModel.onIntent(WalletModeSelectionIntent.UpdateActiveWalletMode(it))
                }
            )
        }
    }

    override val scope: Scope
        get() = payloadScope
}

@Composable
fun WalletModesDialogContent(
    totalBalance: BalanceState,
    portfolioBalanceState: BalanceState,
    defiWalletBalance: BalanceState,
    selectedMode: WalletMode,
    onItemClicked: (WalletMode) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TableRow(
            content = {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.dashboard_total_balance),
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.title
                    )
                    Text(
                        text = (totalBalance as? BalanceState.Data)?.money?.toStringWithSymbol().orEmpty(),
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.title
                    )
                }
            }
        )

        DefaultTableRow(
            primaryText = stringResource(id = R.string.brokerage),
            secondaryText = (portfolioBalanceState as? BalanceState.Data)?.money?.toStringWithSymbol().orEmpty(),
            startImageResource = ImageResource.Local(R.drawable.ic_portfolio),
            onClick = { onItemClicked(WalletMode.CUSTODIAL_ONLY) },
            endImageResource = if (selectedMode == WalletMode.CUSTODIAL_ONLY) ImageResource.Local(
                R.drawable.ic_wallet_mode_selected
            ) else ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_chevron_end
            )
        )

        DefaultTableRow(
            primaryText = stringResource(id = R.string.defi),
            secondaryText = (defiWalletBalance as? BalanceState.Data)?.money?.toStringWithSymbol().orEmpty(),
            startImageResource = ImageResource.Local(R.drawable.ic_defi_wallet),
            onClick = { onItemClicked(WalletMode.NON_CUSTODIAL_ONLY) },
            endImageResource = if (selectedMode == WalletMode.NON_CUSTODIAL_ONLY) ImageResource.Local(
                R.drawable.ic_wallet_mode_selected
            ) else ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_chevron_end
            )
        )
    }
}

@Preview
@Composable
private fun WalletModePreview() {
    AppTheme {
        AppSurface {
            WalletModesDialogContent(
                totalBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 1000.toBigInteger())),
                portfolioBalanceState = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 300.toBigInteger())),
                defiWalletBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 444.toBigInteger())),
                selectedMode = WalletMode.CUSTODIAL_ONLY,
                onItemClicked = {}
            )
        }
    }
}

interface WalletModeChangeHost {
    fun onChangeActiveModeRequested()
}
