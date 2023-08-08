package com.blockchain.home.presentation.fiat.fundsdetail.composable

import android.content.res.Configuration
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Bank
import com.blockchain.componentlib.icons.Cash
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
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
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FiatFundDetail(
    analytics: Analytics = get(),
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
            viewModel.onIntent(
                FiatFundsDetailIntent.FiatAction(
                    account = account,
                    action = AssetAction.FiatDeposit
                )
            )
            analytics.logEvent(DashboardAnalyticsEvents.FiatAddCashClicked(ticker = account.currency.networkTicker))
        },
        withdrawOnClick = { account ->
            viewModel.onIntent(
                FiatFundsDetailIntent.FiatAction(
                    account = account,
                    action = AssetAction.FiatWithdraw
                )
            )
            analytics.logEvent(DashboardAnalyticsEvents.FiatCashOutClicked(ticker = account.currency.networkTicker))
        },
        retryLoadData = {
            viewModel.onIntent(FiatFundsDetailIntent.LoadData)
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
    retryLoadData: () -> Unit,
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
            retryLoadData = retryLoadData,
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
    retryLoadData: () -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SheetHeader(
                    title = detail.name,
                    onClosePress = onBackPressed,
                    startImage = StackedIcon.SingleIcon(
                        ImageResource.Remote(
                            url = detail.logo,
                            size = AppTheme.dimensions.standardSpacing,
                            shape = CircleShape
                        )
                    ),
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                when (data) {
                    DataResource.Loading -> {
                        ShimmerLoadingCard(
                            modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
                            showEndBlocks = false
                        )
                    }

                    is DataResource.Data -> {
                        Text(
                            modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing),
                            text = data.data.balance.toStringWithSymbol(),
                            style = AppTheme.typography.title2,
                            color = AppTheme.colors.title
                        )

                        Surface(
                            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
                            shape = AppTheme.shapes.large,
                            color = Color.Transparent
                        ) {
                            Column {

                                DefaultTableRow(
                                    primaryText = stringResource(
                                        com.blockchain.stringResources.R.string.common_deposit
                                    ),
                                    secondaryText = stringResource(
                                        com.blockchain.stringResources.R.string.fiat_funds_detail_deposit_details
                                    ),
                                    startImageResource = Icons.Filled.Bank.withTint(AppColors.title),
                                    endImageResource = if (data.data.depositEnabled &&
                                        showWithdrawChecksLoading.not()
                                    ) {
                                        Icons.ChevronRight.withTint(AppColors.body)
                                    } else {
                                        ImageResource.None
                                    },
                                    onClick = {
                                        if (data.data.depositEnabled && showWithdrawChecksLoading.not()) {
                                            depositOnClick(detail.account)
                                        }
                                    },
                                    contentAlpha = if (data.data.depositEnabled && !showWithdrawChecksLoading) {
                                        1F
                                    } else {
                                        0.5F
                                    },
                                )

                                AppDivider()

                                Box(contentAlignment = Alignment.Center) {
                                    DefaultTableRow(
                                        primaryText = stringResource(
                                            com.blockchain.stringResources.R.string.common_cash_out
                                        ),
                                        secondaryText = stringResource(
                                            com.blockchain.stringResources.R.string.fiat_funds_detail_withdraw_details
                                        ),
                                        startImageResource = Icons.Filled.Cash.withTint(AppColors.title),
                                        endImageResource = if (data.data.withdrawEnabled &&
                                            showWithdrawChecksLoading.not()
                                        ) {
                                            Icons.ChevronRight.withTint(AppColors.body)
                                        } else {
                                            ImageResource.None
                                        },
                                        onClick = {
                                            if (data.data.withdrawEnabled && showWithdrawChecksLoading.not()) {
                                                withdrawOnClick(detail.account)
                                            }
                                        },
                                        contentAlpha = if (data.data.withdrawEnabled && !showWithdrawChecksLoading) {
                                            1F
                                        } else {
                                            0.5F
                                        },
                                    )

                                    if (showWithdrawChecksLoading) {
                                        LoadingIndicator(
                                            modifier = Modifier.align(Alignment.Center),
                                            color = AppColors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is DataResource.Error -> {
                        SnackbarAlert(
                            message = stringResource(com.blockchain.stringResources.R.string.common_error),
                            type = SnackbarType.Error,
                            actionLabel = stringResource(com.blockchain.stringResources.R.string.common_retry),
                            onActionClicked = retryLoadData
                        )
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
    }
}

@Preview
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
            error = FiatActionErrorState(com.blockchain.stringResources.R.string.fiat_funds_detail_pending_withdrawal)
        },
        retryLoadData = {},
        onBackPressed = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFiatFundDetailScreenDark() {
    PreviewFiatFundDetailScreen()
}

@Preview
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
        retryLoadData = {},
        onBackPressed = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFiatFundDetailScreenLoadingDark() {
    PreviewFiatFundDetailScreenLoading()
}

@Preview
@Composable
fun PreviewFiatFundDetailScreenError() {
    FiatFundDetailScreen(
        detail = DataResource.Data(FiatFundsDetail(NullFiatAccount, "US Dollar", "")),
        data = DataResource.Error(Exception()),
        showWithdrawChecksLoading = true,
        actionError = null,
        depositOnClick = {},
        withdrawOnClick = {},
        retryLoadData = {},
        onBackPressed = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFiatFundDetailScreenErrorDark() {
    PreviewFiatFundDetailScreenError()
}
