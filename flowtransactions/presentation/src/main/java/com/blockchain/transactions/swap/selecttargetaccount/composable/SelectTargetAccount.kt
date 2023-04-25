package com.blockchain.transactions.swap.selecttargetaccount.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.blockchain.betternavigation.navigateTo
import com.blockchain.chrome.setResult
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.transactions.common.accounts.composable.AccountList
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.SwapGraph
import com.blockchain.transactions.swap.selectsource.SelectSourceIntent
import com.blockchain.transactions.swap.selecttarget.TargetAssetNavigationEvent
import com.blockchain.transactions.swap.selecttargetaccount.SelectTargetAccountIntent
import com.blockchain.transactions.swap.selecttargetaccount.SelectTargetAccountViewModel
import com.blockchain.transactions.swap.selecttargetaccount.SelectTargetAccountViewState
import com.blockchain.transactions.swap.selecttargetaccount.TargetAccountNavigationEvent
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoValue
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import java.io.Serializable

data class SelectTargetAccountArgs(
    val sourceTicker: String,
    val targetTicker: String,
    val mode: WalletMode,
) : Serializable

@Composable
fun SelectTargetAccount(
    sourceTicker: String,
    targetTicker: String,
    mode: WalletMode,
    viewModel: SelectTargetAccountViewModel = getViewModel(
        scope = payloadScope,
        key = sourceTicker + targetTicker,
        parameters = { parametersOf(sourceTicker, targetTicker, mode) }
    ),
    accountSelected: (CryptoAccount) -> Unit,
    onClosePressed: () -> Unit,
    onBackPressed: () -> Unit
) {

    val viewState: SelectTargetAccountViewState by viewModel.viewState.collectAsStateLifecycleAware()
    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(SelectTargetAccountIntent.LoadData)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is TargetAccountNavigationEvent.ConfirmSelection -> {
                    accountSelected(navEvent.account)
                    onClosePressed()
                }
            }
        }
    }

    SelectTargetAccountScreen(
        targetTicker = targetTicker,
        accounts = viewState.accountList,
        accountSelected = {
            viewModel.onIntent(SelectTargetAccountIntent.AccountSelected(it.id))
        },
        onClosePressed = onClosePressed,
        onBackPressed = onBackPressed,
    )
}

@Composable
private fun SelectTargetAccountScreen(
    targetTicker: String,
    accounts: DataResource<List<AccountUiElement>>,
    accountSelected: (AccountUiElement) -> Unit,
    onClosePressed: () -> Unit,
    onBackPressed: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_select_account, targetTicker),
            backOnClick = onBackPressed,
            onCloseClick = onClosePressed
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = accounts,
            onAccountClick = accountSelected,
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
