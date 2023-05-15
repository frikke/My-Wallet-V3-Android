package com.dex.presentation.enteramount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.blockchain.chrome.LocalChromePillProvider
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.componentlib.alert.PillAlert
import com.blockchain.componentlib.alert.PillAlertType
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonLoadingIndicator
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.icons.Settings
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green700
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.Orange600
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.DexPrefs
import com.dex.presentation.ALLOWANCE_TRANSACTION_APPROVED
import com.dex.presentation.AmountFieldConfig
import com.dex.presentation.DexTxSubscribeScreen
import com.dex.presentation.SendAndReceiveAmountFields
import com.dex.presentation.graph.ARG_ALLOWANCE_TX
import com.dex.presentation.graph.DexDestination
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
    navController: NavController,
    startReceiving: () -> Unit,
    savedStateHandle: SavedStateHandle?,
    viewModel: DexEnterAmountViewModel = getViewModel(scope = payloadScope),
    dexIntroPrefs: DexPrefs = get(),
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit
) {
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
        subscribe = { viewModel.onIntent(InputAmountIntent.SubscribeForTxUpdates) },
        unsubscribe = { viewModel.onIntent(InputAmountIntent.UnSubscribeToTxUpdates) }
    )
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(InputAmountIntent.InitTransaction)
            }
            if (event == Lifecycle.Event.ON_RESUME && savedStateHandle?.contains(
                    ALLOWANCE_TRANSACTION_APPROVED
                ) == true
            ) {
                savedStateHandle.get<Boolean>(ALLOWANCE_TRANSACTION_APPROVED)?.let {
                    if (it) {
                        viewModel.onIntent(InputAmountIntent.AllowanceTransactionApproved)
                    } else {
                        viewModel.onIntent(InputAmountIntent.AllowanceTransactionDeclined)
                    }
                }
                savedStateHandle.remove<Boolean>(ALLOWANCE_TRANSACTION_APPROVED)
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
                                        com.blockchain.stringResources.R.string.approval_for_token_failed,
                                        event.currencyTicker
                                    )
                                ),
                                icon = Icons.Filled.Alert.withTint(Red400),
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
                                        com.blockchain.stringResources.R.string.approval_for_token_completed,
                                        event.currencyTicker
                                    )
                                ),
                                icon = Icons.Filled.Check.withTint(Green700),
                                type = PillAlertType.Success
                            )
                        )
                    }
                }
            }
        }
    }

    val viewState: InputAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    val spacing = AppTheme.dimensions.smallSpacing
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
    ) {
        item {
            MenuOptionsScreen(
                openSettings = openSettings,
                launchQrScanner = launchQrScanner
            )
        }

        (viewState as? InputAmountViewState.TransactionInputState)?.let {
            paddedItem(paddingValues = PaddingValues(spacing)) {
                InputScreen(
                    selectSourceAccount = {
                        navController.navigate(DexDestination.SelectSourceAccount.route)
                        keyboardController?.hide()
                    },
                    selectDestinationAccount = {
                        navController.navigate(DexDestination.SelectDestinationAccount.route)
                        keyboardController?.hide()
                    },
                    viewState = it,
                    onValueChanged = {
                        viewModel.onIntent(InputAmountIntent.AmountUpdated(it.text))
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
                    },
                    onPreviewClicked = {
                        navController.navigate(DexDestination.Confirmation.route)
                        keyboardController?.hide()
                    },
                    txInProgressDismiss = {
                        viewModel.onIntent(InputAmountIntent.IgnoreTxInProcessError)
                    },
                    revokeAllowance = {
                        viewModel.onIntent(InputAmountIntent.RevokeSourceCurrencyAllowance)
                    }
                )
            }
        }

        (viewState as? InputAmountViewState.NoInputViewState)?.let {
            paddedItem(paddingValues = PaddingValues(spacing)) {
                NoInputScreen(
                    networkSelection = it.networkSelection,
                    selectNetworkOnClick = {
                        navController.navigate(DexDestination.SelectNetwork.route)
                        keyboardController?.hide()
                    },
                    settingsOnClick = {
                        navController.navigate(DexDestination.Settings.route)
                        keyboardController?.hide()
                    },
                    receive = startReceiving
                )
            }
        }
    }
}

@Composable
private fun NoInputScreen(
    networkSelection: DataResource<NetworkSelection>,
    selectNetworkOnClick: () -> Unit,
    settingsOnClick: () -> Unit,
    receive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ((networkSelection as? DataResource.Data)?.data as? NetworkSelection.Available)?.let {
            NetworkSelection(
                logo = it.logo,
                name = it.name,
                networkOnClick = selectNetworkOnClick,
                settingsOnClick = settingsOnClick
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        }

        Surface(
            color = AppTheme.colors.background,
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
                            id = com.blockchain.stringResources.R.string.dex_no_input_title
                        )
                    )
                )
                StandardVerticalSpacer()

                SimpleText(
                    text = stringResource(id = com.blockchain.stringResources.R.string.dex_no_input_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SimpleText(
                    text = stringResource(
                        id = com.blockchain.stringResources.R.string.transfer_from_your_trading_account
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
                    text = stringResource(id = com.blockchain.stringResources.R.string.common_receive),
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
    onPreviewClicked: () -> Unit,
    revokeAllowance: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit,
    settingsOnClick: () -> Unit,
    selectNetworkOnClick: () -> Unit,
    txInProgressDismiss: () -> Unit,
    viewState: InputAmountViewState.TransactionInputState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ((viewState.networkSelection as? DataResource.Data)?.data as? NetworkSelection.Available)?.let {
            NetworkSelection(
                logo = it.logo,
                name = it.name,
                networkOnClick = selectNetworkOnClick,
                settingsOnClick = settingsOnClick
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        }

        viewState.topScreenUiError?.let {
            UiError(
                modifier = Modifier.padding(bottom = AppTheme.dimensions.smallSpacing),
                title = it.title ?: stringResource(
                    id = com.blockchain.stringResources.R.string.common_http_error_title
                ),
                description = it.description ?: stringResource(
                    id = com.blockchain.stringResources.R.string.common_http_error_description
                ),
                close = null
            )
        }

        viewState.txInProgressWarning?.let {
            UiError(
                modifier = Modifier.padding(bottom = AppTheme.dimensions.smallSpacing),
                title = stringResource(id = com.blockchain.stringResources.R.string.tx_in_process),
                description = stringResource(id = com.blockchain.stringResources.R.string.not_accurate_balance),
                close = txInProgressDismiss
            )
        }

        SendAndReceiveAmountFields(
            onValueChanged = onValueChanged,
            sendAmountFieldConfig = AmountFieldConfig(
                isReadOnly = false,
                isEnabled = true,
                exchange = viewState.inputExchangeAmount,
                currency = viewState.sourceCurrency,
                max = viewState.maxAmount,
                onCurrencyClicked = selectSourceAccount,
                amount = viewState.txAmount,
                balance = viewState.sourceAccountBalance,
                canChangeCurrency = viewState.canChangeInputCurrency()
            ),

            receiveAmountFieldConfig = AmountFieldConfig(
                isReadOnly = true,
                isEnabled = viewState.operationInProgress == DexOperation.NONE,
                exchange = viewState.outputExchangeAmount,
                currency = viewState.destinationCurrency,
                max = null,
                onCurrencyClicked = selectDestinationAccount,
                amount = viewState.outputAmount,
                balance = viewState.destinationAccountBalance,
                canChangeCurrency = true
            )
        )

        ((viewState.networkSelection as? DataResource.Data)?.data as? NetworkSelection.Unavailable)?.let {
            Settings(settingsOnClick)
        }

        viewState.uiFee?.takeIf { viewState.operationInProgress == DexOperation.NONE }?.let { uiFee ->
            Fee(uiFee)
        }
        viewState.operationInProgress.takeIf { it == DexOperation.PRICE_FETCHING }?.let {
            PriceFetching()
        }

        viewState.noTokenAllowanceError?.let {
            TokenAllowance(
                onClick = onTokenAllowanceRequested,
                currency = it.token,
                txInProgress = viewState.operationInProgress in listOf(
                    DexOperation.PUSHING_ALLOWANCE_TX,
                    DexOperation.BUILDING_ALLOWANCE_TX
                )
            )
        }

        viewState.allowanceCanBeRevoked.takeIf { it }?.let {
            PrimaryButton(
                state = if (viewState.operationInProgress == DexOperation.PUSHING_ALLOWANCE_TX) {
                    ButtonState.Loading
                } else ButtonState.Enabled,
                modifier = Modifier.padding(all = AppTheme.dimensions.standardSpacing),
                text = "Revoke allowance for ${viewState.sourceCurrency?.displayTicker}",
                onClick = revokeAllowance
            )
        }

        viewState.alertError?.let {
            AlertButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
                    .fillMaxWidth(),
                text = it.message(LocalContext.current),
                onClick = { },
                state = ButtonState.Enabled
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
        text = stringResource(id = com.blockchain.stringResources.R.string.preview_swap),
        onClick = onClick
    )
}

@Composable
private fun TokenAllowance(onClick: () -> Unit, currency: Currency, txInProgress: Boolean) {
    MinimalButton(
        modifier = Modifier
            .padding(top = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
            .background(Color.White, shape = AppTheme.shapes.extraLarge)
            .fillMaxWidth(),
        state = if (txInProgress) ButtonState.Loading else ButtonState.Enabled,
        minHeight = 56.dp,
        icon = Icons.Question.withTint(AppTheme.colors.primary),
        text = stringResource(id = com.blockchain.stringResources.R.string.approve_token, currency.displayTicker),
        onClick = onClick
    )
}

@Preview
@Composable
private fun UiError(
    modifier: Modifier = Modifier,
    title: String = "Transaction In progress",
    description: String = "Your balances may not be accurate. " +
        "Once the transaction is confirmed, your balances will update.",
    close: (() -> Unit)? = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing),
                color = Color.White
            )
            .border(
                width = 1.dp,
                color = Orange600,
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)
            )
    ) {
        Column(Modifier.padding(AppTheme.dimensions.smallSpacing)) {
            Row {
                SimpleText(
                    text = title,
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Warning,
                    gravity = ComposeGravities.Centre
                )
                Spacer(modifier = Modifier.weight(1f))
                if (close != null) {
                    Image(
                        modifier = Modifier.clickable { close() },
                        imageResource = Icons.Close.withTint(Grey400)
                            .withBackground(
                                backgroundColor = Grey000,
                                backgroundSize = AppTheme.dimensions.standardSpacing
                            )
                    )
                }
            }

            SimpleText(
                modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing),
                text = description,
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }
}

@Composable
private fun Fee(uiFee: UiFee) {
    TableRow(
        modifier = Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
            .clip(shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)),
        content = {
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)),
                text = stringResource(id = com.blockchain.stringResources.R.string.estimated_fees),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
        },
        contentStart = {
            Image(
                imageResource = ImageResource.Remote(
                    uiFee.fee.currency.logo,
                    size = 24.dp
                )
            )
        },
        contentEnd = {
            Text(
                text = "~ ${uiFee.feeInFiat?.toStringWithSymbol() ?: uiFee.fee.toStringWithSymbol()}",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.title
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
                text = stringResource(id = com.blockchain.stringResources.R.string.fetching_quote),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
        },
        contentStart = {
            ButtonLoadingIndicator(
                modifier = Modifier.size(24.dp),
                loadingIconResId = com.blockchain.componentlib.R.drawable.ic_loading_minimal_light
            )
        }
    )
}

@Composable
private fun Settings(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
            .fillMaxWidth()
    ) {
        Spacer(Modifier.weight(1f))
        TertiaryButton(
            text = stringResource(id = com.blockchain.stringResources.R.string.common_settings),
            textColor = Grey900,
            onClick = onClick,
            icon = Icons.Settings
        )
    }
}

@Composable
private fun NetworkSelection(
    logo: String,
    name: String,
    networkOnClick: () -> Unit,
    settingsOnClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F),
            color = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
        ) {
            Row(
                Modifier
                    .clickable(onClick = networkOnClick)
                    .padding(AppTheme.dimensions.tinySpacing)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallTagIcon(
                    icon = StackedIcon.SmallTag(
                        main = Icons.Filled.Network.withSize(16.dp),
                        tag = ImageResource.Remote(logo)
                    ),
                    iconBackground = Grey000,
                    tagIconSize = 12.dp
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    modifier = Modifier.weight(1F),
                    text = stringResource(R.string.common_network),
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )

                Text(
                    text = name,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Image(Icons.ChevronRight)
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Surface(
            modifier = Modifier.fillMaxHeight(),
            color = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
        ) {
            Image(
                modifier = Modifier
                    .padding(horizontal = AppTheme.dimensions.smallSpacing)
                    .clickable(onClick = settingsOnClick),
                imageResource = Icons.Settings.withSize(AppTheme.dimensions.standardSpacing)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNetworkSelection() {
    NetworkSelection(
        logo = "", name = "ETH",
        networkOnClick = {}, settingsOnClick = {}
    )
}

@Preview
@Composable
private fun PreviewInputScreen_NetworkSelection() {
    InputScreen(
        {}, {}, {}, {}, {}, {}, {}, {}, {},
        InputAmountViewState.TransactionInputState(
            networkSelection = DataResource.Data(NetworkSelection.Available("", "ETH")),
            sourceCurrency = CryptoCurrency.ETHER,
            destinationCurrency = CryptoCurrency.BTC,
            maxAmount = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal()),
            txAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            operationInProgress = DexOperation.NONE,
            destinationAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            sourceAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 200.toBigDecimal()),
            inputExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            outputExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            outputAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            allowanceCanBeRevoked = false,
            uiFee = UiFee(
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            ),
            previewActionButtonState = ActionButtonState.ENABLED,
        )
    )
}

@Preview
@Composable
private fun PreviewInputScreen_NoNetworkSelection() {
    InputScreen(
        {}, {}, {}, {}, {}, {}, {}, {}, {},
        InputAmountViewState.TransactionInputState(
            networkSelection = DataResource.Data(NetworkSelection.Unavailable),
            sourceCurrency = CryptoCurrency.ETHER,
            destinationCurrency = CryptoCurrency.BTC,
            maxAmount = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal()),
            txAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            operationInProgress = DexOperation.NONE,
            destinationAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            sourceAccountBalance = Money.fromMajor(CryptoCurrency.ETHER, 200.toBigDecimal()),
            inputExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            outputExchangeAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            outputAmount = Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            allowanceCanBeRevoked = false,
            uiFee = UiFee(
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
                Money.fromMajor(CryptoCurrency.ETHER, 20.toBigDecimal()),
            ),
            previewActionButtonState = ActionButtonState.ENABLED,
        )
    )
}
