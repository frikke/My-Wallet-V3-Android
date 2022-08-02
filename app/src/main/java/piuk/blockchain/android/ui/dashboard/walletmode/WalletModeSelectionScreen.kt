package piuk.blockchain.android.ui.dashboard.walletmode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R

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
            portfolioBalanceState = state.brokerageBalance,
            defiWalletBalance = state.defiWalletBalance,
            selectedMode = state.enabledWalletMode,
            onItemClicked = {
                viewModel.onIntent(WalletModeSelectionIntent.ActivateWalletModeRequested(it))
            }
        )
    }
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
                        style = AppTheme.typography.title2,
                        color = AppTheme.colors.title
                    )
                }
            }
        )

        BrokerageWalletModeSelection(
            brokerageWalletBalance = portfolioBalanceState,
            selectedWalletMode = selectedMode,
            onClick = { onItemClicked(WalletMode.CUSTODIAL_ONLY) }
        )

        DefiWalletModeSelection(
            defiWalletBalance = defiWalletBalance,
            selectedWalletMode = selectedMode,
            onClick = { onItemClicked(WalletMode.NON_CUSTODIAL_ONLY) }
        )
    }
}

@Composable
fun BrokerageWalletModeSelection(
    brokerageWalletBalance: BalanceState,
    selectedWalletMode: WalletMode,
    onClick: () -> Unit
) {
    WalletModeSelection(
        balanceState = brokerageWalletBalance,
        requestedWalletMode = WalletMode.CUSTODIAL_ONLY,
        selectedWalletMode = selectedWalletMode,
        walletName = stringResource(id = R.string.brokerage),
        walletIcon = ImageResource.Local(R.drawable.ic_portfolio),
        onClick = onClick
    )
}

@Composable
fun DefiWalletModeSelection(
    defiWalletBalance: BalanceState,
    selectedWalletMode: WalletMode,
    onClick: () -> Unit
) {
    WalletModeSelection(
        balanceState = defiWalletBalance,
        requestedWalletMode = WalletMode.NON_CUSTODIAL_ONLY,
        selectedWalletMode = selectedWalletMode,
        walletName = stringResource(id = R.string.defi),
        walletIcon = ImageResource.Local(R.drawable.ic_defi_wallet),
        onClick = onClick
    )
}

@Composable
fun WalletModeSelection(
    balanceState: BalanceState,
    requestedWalletMode: WalletMode,
    selectedWalletMode: WalletMode,
    walletName: String,
    walletIcon: ImageResource,
    onClick: () -> Unit
) {
    DefaultTableRow(
        primaryText = walletName,
        secondaryText = when (balanceState) {
            BalanceState.ActivationRequired -> stringResource(R.string.defi_onboarding_enable_wallet_title)
            else -> (balanceState as? BalanceState.Data)?.money?.toStringWithSymbol().orEmpty()
        },
        paragraphText = when (balanceState) {
            BalanceState.ActivationRequired -> stringResource(R.string.defi_onboarding_enable_wallet_description)
            else -> null
        },
        startImageResource = walletIcon,
        endImageResource = when (selectedWalletMode) {
            requestedWalletMode -> ImageResource.Local(
                id = R.drawable.ic_wallet_mode_selected,
                colorFilter = ColorFilter.tint(AppTheme.colors.primary)
            )
            else -> ImageResource.Local(R.drawable.ic_chevron_end)
        },
        onClick = onClick
    )
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

@Preview
@Composable
private fun WalletModePreviewEnableWallet() {
    AppTheme {
        AppSurface {
            WalletModesDialogContent(
                totalBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 1000.toBigInteger())),
                portfolioBalanceState = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 300.toBigInteger())),
                defiWalletBalance = BalanceState.ActivationRequired,
                selectedMode = WalletMode.CUSTODIAL_ONLY,
                onItemClicked = {}
            )
        }
    }
}
