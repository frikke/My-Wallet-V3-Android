package com.blockchain.home.presentation.fiat.fundsdetail.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.fiat.actions.FiatActionsIntent
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavEvent
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigation
import com.blockchain.home.presentation.fiat.actions.FiatActionsViewModel
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetail
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailData
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailIntent
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailViewModel
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailViewState
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FiatFundDetail(
    currency: FiatCurrency,
    viewModel: FiatFundsDetailViewModel = getViewModel(
        scope = payloadScope,
        key = currency.networkTicker,
        parameters = { parametersOf(currency) }
    ),
    actionsViewModel: FiatActionsViewModel = getViewModel(scope = payloadScope),
    fiatActionsNavigation: FiatActionsNavigation,
    onBackPressed: () -> Unit
) {
    val viewState: FiatFundsDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(FiatFundsDetailIntent.LoadData)
        onDispose { }
    }

    // navigation
    val lifecycleOwner = LocalLifecycleOwner.current
    val navEventsFlowLifecycleAware = remember(actionsViewModel.navigationEventFlow, lifecycleOwner) {
        actionsViewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is FiatActionsNavEvent.WireTransferAccountDetails -> {
                    fiatActionsNavigation.wireTransferDetail(it.account)
                    onBackPressed()
                }
            }
        }
    }
    //

    FiatFundDetailScreen(
        detail = viewState.detail,
        data = viewState.data,
        depositOnClick = { account ->
            actionsViewModel.onIntent(
                FiatActionsIntent.Deposit(
                    account = account,
                    action = AssetAction.FiatDeposit,
                    shouldLaunchBankLinkTransfer = false
                )
            )
        },
        onBackPressed = onBackPressed
    )
}

@Composable
fun FiatFundDetailScreen(
    detail: DataResource<FiatFundsDetail>,
    data: DataResource<FiatFundsDetailData>,
    depositOnClick: (FiatAccount) -> Unit,
    onBackPressed: () -> Unit
) {
    (detail as? DataResource.Data)?.data?.let {
        FiatFundDetailScreenData(
            detail = it,
            data = data,
            depositOnClick = depositOnClick,
            onBackPressed = onBackPressed
        )
    }
}

@Composable
fun FiatFundDetailScreenData(
    detail: FiatFundsDetail,
    data: DataResource<FiatFundsDetailData>,
    depositOnClick: (FiatAccount) -> Unit,
    onBackPressed: () -> Unit
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(
            title = detail.name,
            onClosePress = onBackPressed,
            startImageResource = ImageResource.Remote(
                url = detail.logo,
                size = AppTheme.dimensions.standardSpacing,
                shape = CircleShape
            ),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        when (data) {
            DataResource.Loading -> {
                ShimmerLoadingCard()
            }
            is DataResource.Data -> {
                Text(
                    modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing),
                    text = data.data.balance.toStringWithSymbol(),
                    style = AppTheme.typography.title2,
                    color = AppTheme.colors.title
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                DefaultTableRow(
                    modifier = Modifier.alpha(if (data.data.depositEnabled) 1F else 0.5F),
                    primaryText = stringResource(R.string.common_deposit),
                    secondaryText = stringResource(R.string.fiat_funds_detail_deposit_details),
                    startImageResource = ImageResource.Local(R.drawable.ic_fiat_deposit),
                    endImageResource = if (data.data.depositEnabled) {
                        ImageResource.Local(R.drawable.ic_chevron_end)
                    } else {
                        ImageResource.None
                    },
                    onClick = { depositOnClick(detail.account) }
                )

                Divider(color = Color(0XFFF1F2F7))

                DefaultTableRow(
                    modifier = Modifier.alpha(if (data.data.withdrawEnabled) 1F else 0.5F),
                    primaryText = stringResource(R.string.common_withdraw),
                    secondaryText = stringResource(R.string.fiat_funds_detail_withdraw_details),
                    startImageResource = ImageResource.Local(R.drawable.ic_fiat_withdraw),
                    endImageResource = if (data.data.withdrawEnabled) {
                        ImageResource.Local(R.drawable.ic_chevron_end)
                    } else {
                        ImageResource.None
                    },
                    onClick = {}
                )
            }
            is DataResource.Error -> {
            }
        }

        Spacer(modifier = Modifier.size(navBarHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFiatFundDetailScreen() {
    FiatFundDetailScreen(
        detail = DataResource.Data(FiatFundsDetail(NullFiatAccount, "US Dollar", "")),
        data = DataResource.Data(
            FiatFundsDetailData(
                balance = Money.fromMajor(CryptoCurrency.ETHER, 12.toBigDecimal()),
                depositEnabled = true,
                withdrawEnabled = false
            )
        ),
        {},
        {}
    )
}
