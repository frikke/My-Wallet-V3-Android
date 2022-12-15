package com.blockchain.home.presentation.fiat.fundsdetail.composable

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonLoadingIndicator
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.fiat.fundsdetail.FiatActionErrorState
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetail
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailData
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailIntent
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailNavEvent
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailViewModel
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailViewState
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FiatFundDetail(
    fiatTicker: String,
    dismiss: () -> Unit
) {
    val viewModel: FiatFundsDetailViewModel = getViewModel(
        scope = payloadScope,
        key = fiatTicker,
        parameters = { parametersOf(fiatTicker) }
    )
    val viewState: FiatFundsDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(FiatFundsDetailIntent.LoadData)
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                FiatFundsDetailNavEvent.Dismss -> dismiss()
            }
        }
    }

    FiatFundDetailScreen(
        detail = viewState.detail,
        data = viewState.data,
        showWithdrawChecksLoading = viewState.showWithdrawChecksLoading,
        actionError = viewState.actionError,
        depositOnClick = { account ->
            //            analytics.logEvent(
            //                fiatAssetAction(AssetDetailsAnalytics.FIAT_DEPOSIT_CLICKED, account.currency.networkTicker)
            //            )
            //            analytics.logEvent(DepositAnalytics.DepositClicked(LaunchOrigin.CURRENCY_PAGE))

            viewModel.onIntent(
                FiatFundsDetailIntent.Deposit(
                    account = account,
                    action = AssetAction.FiatDeposit,
                    shouldLaunchBankLinkTransfer = false
                )
            )
        },
        withdrawOnClick = { account ->
            //            analytics.logEvent(
            //                fiatAssetAction(AssetDetailsAnalytics.FIAT_DEPOSIT_CLICKED, account.currency.networkTicker)
            //            )
            //            analytics.logEvent(DepositAnalytics.DepositClicked(LaunchOrigin.CURRENCY_PAGE))

            viewModel.onIntent(
                FiatFundsDetailIntent.Withdraw(
                    account = account,
                    action = AssetAction.FiatWithdraw,
                    shouldLaunchBankLinkTransfer = false
                )
            )
        },
        onBackPressed = dismiss
    )
}

@Composable
fun FiatFundDetailScreen(
    detail: DataResource<FiatFundsDetail>,
    data: DataResource<FiatFundsDetailData>,
    showWithdrawChecksLoading: Boolean,
    actionError: FiatActionErrorState?,
    depositOnClick: (FiatAccount) -> Unit,
    withdrawOnClick: (FiatAccount) -> Unit,
    onBackPressed: () -> Unit
) {
    (detail as? DataResource.Data)?.data?.let {
        FiatFundDetailScreenData(
            detail = it,
            data = data,
            showWithdrawChecksLoading = showWithdrawChecksLoading,
            actionError = actionError,
            depositOnClick = depositOnClick,
            withdrawOnClick = withdrawOnClick,
            onBackPressed = onBackPressed
        )
    }
}

@Composable
fun FiatFundDetailScreenData(
    detail: FiatFundsDetail,
    data: DataResource<FiatFundsDetailData>,
    showWithdrawChecksLoading: Boolean,
    actionError: FiatActionErrorState?,
    depositOnClick: (FiatAccount) -> Unit,
    withdrawOnClick: (FiatAccount) -> Unit,
    onBackPressed: () -> Unit
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.BottomCenter) {
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
                            modifier = Modifier.alpha(
                                if (data.data.depositEnabled && showWithdrawChecksLoading.not()) 1F else 0.5F
                            ),
                            primaryText = stringResource(R.string.common_deposit),
                            secondaryText = stringResource(R.string.fiat_funds_detail_deposit_details),
                            startImageResource = ImageResource.Local(R.drawable.ic_fiat_deposit),
                            endImageResource = if (data.data.depositEnabled && showWithdrawChecksLoading.not()) {
                                ImageResource.Local(R.drawable.ic_chevron_end)
                            } else {
                                ImageResource.None
                            },
                            onClick = {
                                if (data.data.depositEnabled && showWithdrawChecksLoading.not()) {
                                    depositOnClick(detail.account)
                                }
                            }
                        )

                        Divider(color = Color(0XFFF1F2F7))

                        Box(contentAlignment = Alignment.Center) {
                            DefaultTableRow(
                                modifier = Modifier.alpha(
                                    if (data.data.withdrawEnabled && showWithdrawChecksLoading.not()) 1F else 0.5F
                                ),
                                primaryText = stringResource(R.string.common_withdraw),
                                secondaryText = stringResource(R.string.fiat_funds_detail_withdraw_details),
                                startImageResource = ImageResource.Local(R.drawable.ic_fiat_withdraw),
                                endImageResource = if (data.data.withdrawEnabled && showWithdrawChecksLoading.not()) {
                                    ImageResource.Local(R.drawable.ic_chevron_end)
                                } else {
                                    ImageResource.None
                                },
                                onClick = {
                                    if (data.data.withdrawEnabled && showWithdrawChecksLoading.not()) {
                                        withdrawOnClick(detail.account)
                                    }
                                }
                            )

                            if (showWithdrawChecksLoading) {
                                ButtonLoadingIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    loadingIconResId = R.drawable.ic_loading_minimal_light
                                )
                            }
                        }
                    }
                    is DataResource.Error -> {
                    }
                }
            }

            var savedErrorForAnimation: FiatActionErrorState? by remember {
                mutableStateOf(null)
            }
            actionError?.let { savedErrorForAnimation = it }

            androidx.compose.animation.AnimatedVisibility(
                visible = actionError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SnackbarAlert(
                    message = stringResource(savedErrorForAnimation!!.message),
                    type = SnackbarType.Error
                )
            }
        }

        Spacer(modifier = Modifier.size(navBarHeight))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFiatFundDetailScreen() {
    var error: FiatActionErrorState? by remember {
        mutableStateOf(null)
    }

    FiatFundDetailScreen(
        detail = DataResource.Data(FiatFundsDetail(NullFiatAccount, "US Dollar", "")),
        data = DataResource.Data(
            FiatFundsDetailData(
                balance = Money.fromMajor(CryptoCurrency.ETHER, 12.toBigDecimal()),
                depositEnabled = true,
                withdrawEnabled = true
            )
        ),
        showWithdrawChecksLoading = false,
        actionError = error,
        depositOnClick = {
            error = null
        },
        withdrawOnClick = {
            error = FiatActionErrorState(R.string.fiat_funds_detail_pending_withdrawal)
        },
        onBackPressed = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFiatFundDetailScreenLoading() {
    FiatFundDetailScreen(
        detail = DataResource.Data(FiatFundsDetail(NullFiatAccount, "US Dollar", "")),
        data = DataResource.Data(
            FiatFundsDetailData(
                balance = Money.fromMajor(CryptoCurrency.ETHER, 12.toBigDecimal()),
                depositEnabled = true,
                withdrawEnabled = false
            )
        ),
        showWithdrawChecksLoading = true,
        actionError = null,
        depositOnClick = {},
        withdrawOnClick = {},
        onBackPressed = {}
    )
}
