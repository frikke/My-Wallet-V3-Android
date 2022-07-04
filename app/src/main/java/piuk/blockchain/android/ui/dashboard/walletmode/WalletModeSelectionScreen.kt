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
            showEnableDeFiMessage = state.showEnableDeFiMessage,
            totalBalance = state.totalBalance,
            portfolioBalanceState = state.brokerageBalance,
            defiWalletBalance = state.defiWalletBalance,
            selectedMode = state.enabledWalletMode,
            onItemClicked = {
                viewModel.onIntent(WalletModeSelectionIntent.ActivateWalletMode(it))
            },
            enableDeFiClicked = {
                viewModel.onIntent(WalletModeSelectionIntent.EnableDeFiWallet)
            }
        )
    }
}

@Composable
fun WalletModesDialogContent(
    showEnableDeFiMessage: Boolean,
    totalBalance: BalanceState,
    portfolioBalanceState: BalanceState,
    defiWalletBalance: BalanceState,
    selectedMode: WalletMode,
    onItemClicked: (WalletMode) -> Unit,
    enableDeFiClicked: () -> Unit,
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

        if (showEnableDeFiMessage) {
            EnableDeFiTableRow(
                onClick = enableDeFiClicked
            )
        } else {
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
}

@Composable
fun EnableDeFiTableRow(
    onClick: () -> Unit
) {
    DefaultTableRow(
        primaryText = stringResource(id = R.string.defi),
        secondaryText = stringResource(R.string.defi_onboarding_enable_wallet_title),
        paragraphText = stringResource(R.string.defi_onboarding_enable_wallet_description),
        startImageResource = ImageResource.Local(R.drawable.ic_defi_wallet),
        onClick = onClick,
        endImageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_chevron_end)
    )
}

@Preview
@Composable
private fun WalletModePreview() {
    AppTheme {
        AppSurface {
            WalletModesDialogContent(
                showEnableDeFiMessage = false,
                totalBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 1000.toBigInteger())),
                portfolioBalanceState = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 300.toBigInteger())),
                defiWalletBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 444.toBigInteger())),
                selectedMode = WalletMode.CUSTODIAL_ONLY,
                onItemClicked = {},
                enableDeFiClicked = {}
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
                showEnableDeFiMessage = true,
                totalBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 1000.toBigInteger())),
                portfolioBalanceState = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 300.toBigInteger())),
                defiWalletBalance = BalanceState.Data(Money.fromMinor(FiatCurrency.Dollars, 444.toBigInteger())),
                selectedMode = WalletMode.CUSTODIAL_ONLY,
                onItemClicked = {},
                enableDeFiClicked = {}
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEnableDeFiTableRow() {
    AppTheme {
        AppSurface {
            EnableDeFiTableRow(
                onClick = {}
            )
        }
    }
}
