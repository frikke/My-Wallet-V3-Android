package com.dex.presentation.enteramount

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.chrome.LocalChromePillProvider
import com.blockchain.chrome.LocalNavControllerProvider
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.componentlib.alert.PillAlert
import com.blockchain.componentlib.alert.PillAlertType
import com.blockchain.componentlib.anim.AnimatedAmountCounter
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.PrimarySmallButton
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Gas
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.icons.Settings
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.conditional
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.data.DataResource
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.DexPrefs
import com.blockchain.presentation.urllinks.URL_LEARN_MORE_DEX_NOT_ELIGIBLE
import com.blockchain.stringResources.R
import com.dex.domain.ExchangeAmount
import com.dex.presentation.ALLOWANCE_TRANSACTION_APPROVED
import com.dex.presentation.AmountFieldConfig
import com.dex.presentation.DEPOSIT_FOR_ACCOUNT_REQUESTED
import com.dex.presentation.DexAnalyticsEvents
import com.dex.presentation.DexTxSubscribeScreen
import com.dex.presentation.SendAndReceiveAmountFields
import com.dex.presentation.graph.ARG_ALLOWANCE_TX
import com.dex.presentation.graph.ARG_CURRENCY_TICKER
import com.dex.presentation.graph.DexDestination
import com.dex.presentation.network.DexNetworkViewState
import com.dex.presentation.uierrors.DexUiError
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DexEnterAmountScreen(
    listState: LazyListState,
    savedStateHandle: SavedStateHandle?,
    viewModel: DexEnterAmountViewModel = getViewModel(scope = payloadScope),
    dexIntroPrefs: DexPrefs = get(),
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    analytics: Analytics = get(),
) {
    val navController = LocalNavControllerProvider.current

    LaunchedEffect(Unit) {
        if (!dexIntroPrefs.dexIntroShown) {
            navController.navigate(DexDestination.Intro.route)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val chromePillProvider = LocalChromePillProvider.current

    DexTxSubscribeScreen(
        subscribe = {
            viewModel.onIntent(InputAmountIntent.SubscribeForTxUpdates)
            viewModel.onIntent(InputAmountIntent.RevalidateTransaction)
        },
        unsubscribe = { viewModel.onIntent(InputAmountIntent.UnSubscribeToTxUpdates) }
    )
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(InputAmountIntent.InitTransaction)
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                if (savedStateHandle?.contains(ALLOWANCE_TRANSACTION_APPROVED) == true) {
                    savedStateHandle.get<Boolean>(ALLOWANCE_TRANSACTION_APPROVED)?.let {
                        if (it) {
                            viewModel.onIntent(InputAmountIntent.AllowanceTransactionApproved)
                        } else {
                            viewModel.onIntent(InputAmountIntent.AllowanceTransactionDeclined)
                        }
                    }
                    savedStateHandle.remove<Boolean>(ALLOWANCE_TRANSACTION_APPROVED)
                }

                if (savedStateHandle?.contains(DEPOSIT_FOR_ACCOUNT_REQUESTED) == true) {
                    viewModel.onIntent(InputAmountIntent.DepositOnSourceAccountRequested)
                    savedStateHandle.remove<Boolean>(DEPOSIT_FOR_ACCOUNT_REQUESTED)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val uriHandler = LocalUriHandler.current

    val successColor = AppColors.success
    val errorColor = AppColors.error
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest { event ->
            when (event) {
                is AmountNavigationEvent.ApproveAllowanceTx -> navController.navigate(
                    DexDestination.TokenAllowanceSheet.routeWithArgs(
                        listOf(
                            NavArgument(
                                key = ARG_ALLOWANCE_TX,
                                value = Json.encodeToString(AllowanceTxUiData.serializer(), event.data)
                            )
                        )
                    )
                )

                is AmountNavigationEvent.AllowanceTxFailed -> {
                    scope.launch {
                        chromePillProvider.show(
                            PillAlert(
                                text = TextValue.StringValue(
                                    context.getString(
                                        R.string.approval_for_token_failed,
                                        event.currencyTicker
                                    )
                                ),
                                icon = Icons.Filled.Alert.withTint(errorColor),
                                type = PillAlertType.Error
                            )
                        )
                    }
                }

                is AmountNavigationEvent.AllowanceTxCompleted -> {
                    scope.launch {
                        chromePillProvider.show(
                            PillAlert(
                                text = TextValue.StringValue(
                                    context.getString(
                                        R.string.approval_for_token_completed,
                                        event.currencyTicker
                                    )
                                ),
                                icon = Icons.Filled.Check.withTint(successColor),
                                type = PillAlertType.Success
                            )
                        )
                    }
                    analytics.logEvent(DexAnalyticsEvents.ApproveTokenConfirmed)
                }

                is AmountNavigationEvent.AllowanceTxUrl -> uriHandler.openUri(event.url)
                AmountNavigationEvent.ReceiveOnAccount -> {
                    navController.navigate(DexDestination.ReceiveAccountDetail.route)
                }
            }
        }
    }

    val viewState: InputAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
            .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
    ) {
        item {
            MenuOptionsScreen(
                openSettings = openSettings,
                launchQrScanner = launchQrScanner
            )
        }

        (viewState as? InputAmountViewState.TransactionInputState)?.let { viewState ->
            paddedItem(
                paddingValues = {
                    PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
                }
            ) {
                InputScreen(
                    selectSourceAccount = {
                        navController.navigate(DexDestination.SelectSourceAccount.route)
                        keyboardController?.hide()
                    },
                    viewAllowanceTx = { asset, tx ->
                        viewModel.onIntent(InputAmountIntent.ViewAllowanceTx(asset, tx))
                    },
                    selectDestinationAccount = {
                        navController.navigate(DexDestination.SelectDestinationAccount.route)
                        keyboardController?.hide()
                    },
                    viewState = viewState,
                    onInputValueChanged = {
                        viewModel.onIntent(InputAmountIntent.InputAmountUpdated(it))
                        viewState.sourceCurrency?.networkTicker?.let { networkTicker ->
                            analytics.logEvent(DexAnalyticsEvents.AmountEntered(sourceTicker = networkTicker))
                        }
                    },
                    onOutputValueChanged = {
                        viewModel.onIntent(InputAmountIntent.OutputAmountUpdated(it))
                    },
                    settingsOnClick = {
                        navController.navigate(DexDestination.Settings.route)
                        keyboardController?.hide()
                    },
                    selectNetworkOnClick = {
                        navController.navigate(DexDestination.SelectNetwork.route)
                        keyboardController?.hide()
                    },
                    onTokenAllowanceRequested = {
                        viewModel.onIntent(InputAmountIntent.BuildAllowanceTransaction)
                        analytics.logEvent(DexAnalyticsEvents.ApproveTokenClicked)
                    },
                    onPreviewClicked = {
                        navController.navigate(DexDestination.Confirmation.route)
                        keyboardController?.hide()
                    },
                    revokeAllowance = {
                        viewModel.onIntent(InputAmountIntent.RevokeSourceCurrencyAllowance)
                    },
                    onTokenAllowanceApproveButPending = {
                        viewModel.onIntent(InputAmountIntent.PollForPendingAllowance(it))
                    },
                    receive = {
                        viewModel.onIntent(InputAmountIntent.DepositOnAccountRequested(account = it))
                    },
                    onAlertErrorClicked = { title, description ->
                        navController.navigate(
                            DexDestination.DexExtraInfoSheet.routeWithTitleAndDescription(
                                title = title,
                                description = description
                            )
                        )
                    },
                    requestDepositOnCurrency = {
                        viewModel.onIntent(InputAmountIntent.ReceiveCurrencyRequested(it))
                    },
                    onNoSourceAccountFunds = {
                        navController.navigate(
                            DexDestination.NoFundsForSourceAccount.routeWithArgs(
                                listOf(
                                    NavArgument(
                                        key = ARG_CURRENCY_TICKER,
                                        value = it.networkTicker
                                    )
                                )
                            )
                        )
                        keyboardController?.hide()
                    }
                )
            }
        }

        (viewState as? InputAmountViewState.NoInputViewState)?.let {
            paddedItem(
                paddingValues = {
                    PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
                }
            ) {
                NoInputScreen(
                    networkSelection = it.selectedNetwork,
                    allowNetworkSelection = it.allowNetworkSelection,
                    selectNetworkOnClick = {
                        navController.navigate(DexDestination.SelectNetwork.route)
                        keyboardController?.hide()
                    },
                    settingsOnClick = {
                        navController.navigate(DexDestination.Settings.route)
                        keyboardController?.hide()
                    },
                    receive = {
                        navController.navigate(DexDestination.ReceiveAccounts.route)
                    }
                )
            }
        }

        (viewState as? InputAmountViewState.NotEligible)?.let {
            paddedItem(
                paddingValues = {
                    PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
                }
            ) {
                NotEligibleScreen(
                    reason = it.reason ?: stringResource(id = R.string.default_dex_not_available_message),
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotEligibleScreen(
    reason: String = "The DEX is not yet available for your account.",
) {
    val context = LocalContext.current

    Surface(
        color = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = AppTheme.dimensions.smallSpacing
                )
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StandardVerticalSpacer()

            SmallTagIcon(
                icon = StackedIcon.SmallTag(
                    main = Icons.Swap.withTint(AppColors.title)
                        .withBackground(
                            backgroundColor = AppColors.light,
                            iconSize = 58.dp,
                            backgroundSize = 88.dp
                        ),
                    tag = Icons.AlertOn.withTint(AppColors.warning)
                ),
                mainIconSize = 88.dp,
                tagIconSize = 44.dp
            )

            StandardVerticalSpacer()

            SimpleText(
                text = stringResource(id = R.string.currently_unavailable),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                text = reason,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
                modifier = Modifier.padding(
                    vertical = AppTheme.dimensions.smallSpacing
                )
            )

            SmallVerticalSpacer()

            MinimalPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.common_learn_more)
            ) {
                context.openUrl(URL_LEARN_MORE_DEX_NOT_ELIGIBLE)
            }
            SmallVerticalSpacer()
        }
    }
}

@Composable
private fun NoInputScreen(
    networkSelection: DataResource<DexNetworkViewState>,
    allowNetworkSelection: Boolean,
    selectNetworkOnClick: () -> Unit,
    settingsOnClick: () -> Unit,
    receive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        NetworkSelection(
            selectedNetwork = networkSelection,
            allowNetworkSelection = allowNetworkSelection,
            networkOnClick = selectNetworkOnClick,
            settingsOnClick = settingsOnClick
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Surface(
            color = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing
                    )
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StandardVerticalSpacer()
                Image(
                    imageResource = ImageResource.Local(
                        id = com.blockchain.componentlib.R.drawable.ic_empty_state_deposit,
                        contentDescription = stringResource(
                            id = R.string.dex_no_input_title
                        )
                    )
                )
                StandardVerticalSpacer()

                SimpleText(
                    text = stringResource(id = R.string.dex_no_input_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SimpleText(
                    text = stringResource(
                        id = R.string.transfer_from_your_trading_account
                    ),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing
                    )
                )

                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = AppTheme.dimensions.standardSpacing,
                            horizontal = AppTheme.dimensions.smallSpacing
                        ),
                    text = stringResource(id = R.string.common_receive),
                    onClick = receive
                )
            }
        }
    }
}

@Composable
fun InputScreen(
    selectSourceAccount: () -> Unit,
    selectDestinationAccount: () -> Unit,
    onTokenAllowanceRequested: () -> Unit,
    onTokenAllowanceApproveButPending: (AssetInfo) -> Unit,
    onPreviewClicked: () -> Unit,
    onAlertErrorClicked: (String, String) -> Unit,
    revokeAllowance: () -> Unit,
    onInputValueChanged: (String) -> Unit,
    onOutputValueChanged: (String) -> Unit,
    settingsOnClick: () -> Unit,
    viewAllowanceTx: (AssetInfo, String) -> Unit,
    selectNetworkOnClick: () -> Unit,
    receive: (CryptoNonCustodialAccount) -> Unit,
    requestDepositOnCurrency: (Currency) -> Unit,
    onNoSourceAccountFunds: (Currency) -> Unit,
    viewState: InputAmountViewState.TransactionInputState
) {

    LaunchedEffect(key1 = viewState.sourceAccountHasNoFunds, block = {
        if (viewState.sourceAccountHasNoFunds) {
            viewState.sourceCurrency?.let {
                onNoSourceAccountFunds(it)
            }
        }
    })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        NetworkSelection(
            selectedNetwork = viewState.selectedNetwork,
            allowNetworkSelection = viewState.allowNetworkSelection,
            networkOnClick = selectNetworkOnClick,
            settingsOnClick = settingsOnClick
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        SendAndReceiveAmountFields(
            sendAmountFieldConfig = AmountFieldConfig(
                isReadOnly = false,
                isEnabled = (viewState.operationInProgress as? DexOperation.PriceFetching)?.amount?.let {
                    it !is ExchangeAmount.BuyAmount
                } ?: true,
                onValueChanged = onInputValueChanged,
                exchange = viewState.sellExchangeAmount,
                currency = viewState.sourceCurrency,
                max = viewState.maxSourceAmount,
                onCurrencyClicked = selectSourceAccount,
                amount = viewState.sellAmount,
                balance = viewState.sourceAccountBalance,
                canChangeCurrency = viewState.canChangeInputCurrency()
            ),
            receiveAmountFieldConfig = AmountFieldConfig(
                isReadOnly = viewState.destinationCurrency == null,
                isEnabled = (viewState.operationInProgress as? DexOperation.PriceFetching)?.amount?.let {
                    it !is ExchangeAmount.SellAmount
                } ?: true,
                exchange = viewState.buyExchangeAmount,
                currency = viewState.destinationCurrency,
                onValueChanged = onOutputValueChanged,
                max = null,
                onCurrencyClicked = selectDestinationAccount,
                amount = viewState.buyAmount,
                balance = viewState.destinationAccountBalance,
                canChangeCurrency = true
            )
        )

        viewState.uiFee.takeIf { viewState.operationInProgress == DexOperation.None }?.let { uiFee ->
            Fee(uiFee)
        }
        viewState.operationInProgress.takeIf { it is DexOperation.PriceFetching }?.let {
            PriceFetching()
        }

        viewState.noTokenAllowanceError?.let { allowanceError ->
            TokenAllowance(
                onClick = onTokenAllowanceRequested,
                currency = allowanceError.token,
                txInProgress = viewState.allowanceTransactionInProgress,
                assetApprovedButPending = (viewState.operationInProgress as? DexOperation.PollingAllowance)?.asset,
                onViewClicked = (viewState.operationInProgress as? DexOperation.PollingAllowance)?.let {
                    if (it.txId != null) {
                        {
                            viewAllowanceTx(
                                it.asset,
                                it.txId
                            )
                        }
                    } else null
                }
            )
            LaunchedEffect(key1 = allowanceError.hasBeenApproved) {
                if (allowanceError.hasBeenApproved) {
                    onTokenAllowanceApproveButPending(allowanceError.token)
                }
            }
        }

        viewState.allowanceCanBeRevoked.takeIf { it }?.let {
            PrimaryButton(
                state = if (viewState.operationInProgress == DexOperation.PushingAllowance) {
                    ButtonState.Loading
                } else ButtonState.Enabled,
                modifier = Modifier.padding(all = AppTheme.dimensions.standardSpacing),
                text = "Revoke allowance for ${viewState.sourceCurrency?.displayTicker}",
                onClick = revokeAllowance
            )
        }

        viewState.alertError?.let { error ->
            AlertButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
                    .fillMaxWidth(),
                text = error.message(LocalContext.current),
                onClick = {
                    val uiError = (error as? DexUiError.CommonUiError)
                    safeLet(uiError?.title, uiError?.description) { title, description ->
                        onAlertErrorClicked(title, description)
                    }
                },
                state = ButtonState.Enabled
            )
        }

        (viewState.alertError as? DexUiError.InsufficientFunds)?.let {
            MinimalPrimaryButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.smallest_spacing))
                    .fillMaxWidth(),
                text = stringResource(id = R.string.deposit_more, it.account.currency.displayTicker),
                onClick = {
                    receive(it.account)
                }
            )
        }

        (viewState.alertError as? DexUiError.NotEnoughGas)?.let {
            MinimalPrimaryButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.smallest_spacing))
                    .fillMaxWidth(),
                text = stringResource(id = R.string.deposit_more, it.gasCurrency.displayTicker),
                onClick = {
                    requestDepositOnCurrency(it.gasCurrency)
                }
            )
        }

        viewState.previewActionButtonState.takeIf { it != ActionButtonState.INVISIBLE }?.let { state ->
            PreviewSwapButton(
                state = if (state == ActionButtonState.ENABLED) ButtonState.Enabled else ButtonState.Disabled,
                onClick = onPreviewClicked
            )
        }
    }
}

@Composable
private fun PreviewSwapButton(onClick: () -> Unit, state: ButtonState) {
    PrimaryButton(
        modifier = Modifier
            .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
            .fillMaxWidth(),
        state = state,
        text = stringResource(id = R.string.preview_swap),
        onClick = onClick
    )
}

@Composable
private fun TokenAllowance(
    onClick: () -> Unit,
    currency: Currency,
    txInProgress: Boolean,
    assetApprovedButPending: AssetInfo?,
    onViewClicked: (() -> Unit)?
) {
    MinimalPrimaryButton(
        modifier = Modifier
            .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
            .fillMaxWidth(),
        state = if (txInProgress) ButtonState.Loading else ButtonState.Enabled,
        text = stringResource(id = R.string.approve_token, currency.displayTicker),
        onClick = onClick
    )

    if (assetApprovedButPending != null) {
        ApprovingTokenCard(currency.displayTicker, onViewClicked)
    }
}

@Composable
private fun ApprovingTokenCard(ticker: String, onViewClicked: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = com.blockchain.common.R.dimen.small_spacing))
            .background(color = AppTheme.colors.backgroundSecondary, shape = AppTheme.shapes.large)
            .padding(all = dimensionResource(id = com.blockchain.common.R.dimen.small_spacing))
    ) {
        Column {
            SimpleText(
                text = stringResource(id = R.string.approving_token, ticker),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallestSpacing))
            SimpleText(
                text = stringResource(id = R.string.could_take_few_seconds),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
        onViewClicked?.let {
            Spacer(modifier = Modifier.weight(1f))
            PrimarySmallButton(
                text = stringResource(id = R.string.view)
            ) {
                it()
            }
        }
    }
}

@Composable
private fun Fee(uiFee: UiNetworkFee) {
    TableRow(
        modifier = Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
            .clip(shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)),
        content = {
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)),
                text = stringResource(id = R.string.estimated_fees),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
        },
        contentStart = {
            Image(
                imageResource = Icons.Gas.withTint(AppColors.title)
            )
        },
        contentEnd = {
            AnimatedAmountCounter(
                amountText = uiFee.uiText,
                style = ComposeTypographies.Paragraph2SlashedZero,
                color = uiFee.textColor,
                gravity = ComposeGravities.Start
            )
        }
    )
}

@Composable
private fun PriceFetching() {
    TableRow(
        modifier = Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
            .clip(shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)),
        content = {
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)),
                text = stringResource(id = R.string.fetching_quote),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
        },
        contentStart = {
            LoadingIndicator(color = AppColors.primary)
        }
    )
}

@Composable
private fun NetworkSelection(
    selectedNetwork: DataResource<DexNetworkViewState>,
    allowNetworkSelection: Boolean,
    networkOnClick: () -> Unit,
    settingsOnClick: () -> Unit
) {
    val network = (selectedNetwork as? DataResource.Data)?.data

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F),
            color = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
        ) {
            Row(
                Modifier
                    .conditional(allowNetworkSelection) {
                        clickable(onClick = networkOnClick)
                    }
                    .graphicsLayer {
                        alpha = network?.let { 1F } ?: 0.5F
                    }
                    .padding(AppTheme.dimensions.tinySpacing)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallTagIcon(
                    icon = StackedIcon.SmallTag(
                        main = Icons.Filled.Network.withSize(16.dp).withTint(AppColors.title),
                        tag = network?.logo?.let {
                            ImageResource.Remote(it)
                        } ?: Icons.Sync
                    ),
                    iconBackground = AppColors.light,
                    tagIconSize = 12.dp
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    modifier = Modifier.weight(1F),
                    text = stringResource(R.string.common_network),
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )

                network?.name?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                }

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                if (allowNetworkSelection) {
                    Image(Icons.ChevronRight.withTint(AppColors.title))
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Surface(
            modifier = Modifier.fillMaxHeight(),
            color = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
        ) {
            Image(
                modifier = Modifier
                    .padding(horizontal = AppTheme.dimensions.smallSpacing)
                    .clickable(onClick = settingsOnClick),
                imageResource = Icons.Settings
                    .withSize(AppTheme.dimensions.standardSpacing)
                    .withTint(AppColors.title)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNetworkSelection() {
    NetworkSelection(
        selectedNetwork = DataResource.Data(
            DexNetworkViewState(
                chainId = 0, logo = "", name = "Ethereum", selected = true
            )
        ),
        allowNetworkSelection = true,
        networkOnClick = {}, settingsOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNetworkSelectionDark() {
    PreviewNetworkSelection()
}

@Preview
@Composable
private fun PreviewNetworkSelection_Single() {
    NetworkSelection(
        selectedNetwork = DataResource.Data(
            DexNetworkViewState(
                chainId = 0, logo = "", name = "Ethereum", selected = true
            )
        ),
        allowNetworkSelection = false,
        networkOnClick = {}, settingsOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNetworkSelection_SingleDark() {
    PreviewNetworkSelection_Single()
}

@Preview
@Composable
private fun PreviewNetworkSelection_Loading() {
    NetworkSelection(
        selectedNetwork = DataResource.Loading,
        allowNetworkSelection = true,
        networkOnClick = {}, settingsOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNetworkSelection_LoadingDark() {
    PreviewNetworkSelection_Loading()
}

@Preview
@Composable
private fun PreviewInputScreen_NetworkSelection() {
    InputScreen(
        {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, { }, { _, _ -> }, {}, {}, { }, { },
        InputAmountViewState.TransactionInputState(
            selectedNetwork = DataResource.Data(
                DexNetworkViewState(
                    chainId = 0, logo = "", name = "Ethereum", selected = true
                )
            ),
            allowNetworkSelection = true,
            sourceCurrency = CryptoCurrency.ETHER,
            destinationCurrency = CryptoCurrency.BTC,
            maxSourceAmount = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal()),
            sellAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            operationInProgress = DexOperation.None,
            destinationAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            sourceAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 200.toBigDecimal()),
            sellExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            buyExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            buyAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            allowanceCanBeRevoked = false,
            uiFee = UiNetworkFee.DefinedFee(
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            ),
            previewActionButtonState = ActionButtonState.ENABLED,
            errors = listOf(
                DexUiError.TokenNotAllowed(CryptoCurrency.ETHER, false)
            ),
            sourceAccountHasNoFunds = false
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewInputScreen_NetworkSelectionDark() {
    PreviewInputScreen_NetworkSelection()
}
