package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
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
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.icons.Settings
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.DexPrefs
import com.dex.presentation.enteramount.ActionButtonState
import com.dex.presentation.enteramount.AlertError
import com.dex.presentation.enteramount.AllowanceTxUiData
import com.dex.presentation.enteramount.AmountNavigationEvent
import com.dex.presentation.enteramount.DexEnterAmountViewModel
import com.dex.presentation.enteramount.DexOperation
import com.dex.presentation.enteramount.DexUiError
import com.dex.presentation.enteramount.InputAmountIntent
import com.dex.presentation.enteramount.InputAmountViewState
import com.dex.presentation.enteramount.UiFee
import com.dex.presentation.graph.ARG_ALLOWANCE_TX
import com.dex.presentation.graph.DexDestination
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.collectLatest
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
    dexIntroPrefs: DexPrefs = get()
) {
    LaunchedEffect(Unit) {
        if (!dexIntroPrefs.dexIntroShown) {
            navController.navigate(DexDestination.Intro.route)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val lifecycleOwner = LocalLifecycleOwner.current

    DexTxSubscribeScreen(
        subscribe = { viewModel.onIntent(InputAmountIntent.SubscribeForTxUpdates) },
        unsubscribe = { viewModel.onIntent(InputAmountIntent.UnSubscribeToTxUpdates) }
    )

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
                        /*
                        * handle decline
                        * */
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
                            ),
                        )
                    )
                )
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

        (viewState as? InputAmountViewState.TransactionInputState)?.let {
            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
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
                    onTokenAllowanceRequested = {
                        viewModel.onIntent(InputAmountIntent.BuildAllowanceTransaction)
                    },
                    onPreviewClicked = {
                        navController.navigate(DexDestination.Confirmation.route)
                        keyboardController?.hide()
                    }
                )
            }
        }
        (viewState as? InputAmountViewState.NoInputViewState)?.let {
            paddedItem(paddingValues = PaddingValues(spacing)) {
                NoInputScreen(
                    receive = startReceiving
                )
            }
        }
    }
}

@Composable
private fun NoInputScreen(receive: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 3.dp
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
                        id = R.drawable.ic_empty_state_deposit,
                        contentDescription = stringResource(id = R.string.dex_no_input_title),
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
                    text = stringResource(id = R.string.transfer_from_your_trading_account),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing,
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
    onPreviewClicked: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit,
    settingsOnClick: () -> Unit,
    viewState: InputAmountViewState.TransactionInputState
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        (viewState.error as? DexUiError.CommonUiError)?.let {
            UiError(it)
        }

        SourceAndDestinationAmountFields(
            onValueChanged = onValueChanged,
            sourceAmountFieldConfig = AmountFieldConfig(
                isReadOnly = false,
                isEnabled = true,
                exchange = viewState.inputExchangeAmount,
                currency = viewState.sourceCurrency,
                max = viewState.maxAmount,
                onCurrencyClicked = selectSourceAccount,
                amount = viewState.txAmount,
                balance = viewState.sourceAccountBalance,
                canChangeCurrency = true
            ),

            destinationAmountFieldConfig = AmountFieldConfig(
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

        Settings(settingsOnClick)

        viewState.uiFee?.takeIf { viewState.operationInProgress == DexOperation.NONE }?.let { uiFee ->
            Fee(uiFee)
        }
        viewState.operationInProgress.takeIf { it == DexOperation.PRICE_FETCHING }?.let {
            PriceFetching()
        }

        (viewState.error as? DexUiError.TokenNotAllowed)?.let {
            TokenAllowance(
                onClick = onTokenAllowanceRequested,
                currency = it.token,
                txInProgress = viewState.operationInProgress in listOf(
                    DexOperation.PUSHING_ALLOWANCE_TX,
                    DexOperation.BUILDING_ALLOWANCE_TX
                )
            )
        }

        (viewState.error as? AlertError)?.let {
            AlertButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.small_spacing))
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
            .padding(top = dimensionResource(id = R.dimen.small_spacing))
            .fillMaxWidth(),
        state = state,
        text = stringResource(id = R.string.preview_swap), onClick = onClick
    )
}

@Composable
private fun TokenAllowance(onClick: () -> Unit, currency: Currency, txInProgress: Boolean) {
    MinimalButton(
        modifier = Modifier
            .padding(top = dimensionResource(id = R.dimen.small_spacing))
            .background(Color.White, shape = AppTheme.shapes.extraLarge)
            .fillMaxWidth(),
        state = if (txInProgress) ButtonState.Loading else ButtonState.Enabled,
        minHeight = 56.dp,
        icon = Icons.Question.withTint(AppTheme.colors.primary),
        text = stringResource(id = R.string.approve_token, currency.displayTicker),
        onClick = onClick
    )
}

@Composable
private fun UiError(dexUiError: DexUiError.CommonUiError) {
    TableRow(
        modifier = Modifier
            .padding(bottom = AppTheme.dimensions.smallSpacing)
            .clip(shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)),
        content = {
            Column {
                Text(
                    text = dexUiError.title,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
                Text(
                    text = dexUiError.description,
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.body
                )
            }
        }
    )
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
                    .padding(start = dimensionResource(id = R.dimen.tiny_spacing)),
                text = stringResource(id = R.string.estimated_fees),
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
                    .padding(start = dimensionResource(id = R.dimen.tiny_spacing)),
                text = stringResource(id = R.string.fetching_quote),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
        },
        contentStart = {
            ButtonLoadingIndicator(
                modifier = Modifier.size(24.dp),
                loadingIconResId = R.drawable.ic_loading_minimal_light
            )
        },
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
            text = stringResource(id = R.string.common_settings),
            textColor = Grey900,
            onClick = onClick,
            icon = Icons.Settings
        )
    }
}
