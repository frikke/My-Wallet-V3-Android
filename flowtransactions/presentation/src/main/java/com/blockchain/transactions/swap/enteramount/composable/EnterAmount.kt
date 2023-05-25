package com.blockchain.transactions.swap.enteramount.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
import com.blockchain.betternavigation.NavContext
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallTertiaryButton
import com.blockchain.componentlib.card.HorizontalAssetAction
import com.blockchain.componentlib.card.TwoAssetActionHorizontal
import com.blockchain.componentlib.card.TwoAssetActionHorizontalLoading
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.TwoCurrenciesInput
import com.blockchain.componentlib.control.isEmpty
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.transactions.swap.SwapAnalyticsEvents
import com.blockchain.transactions.swap.SwapGraph
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.enteramount.EnterAmountAssetState
import com.blockchain.transactions.swap.enteramount.EnterAmountAssets
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountNavigationEvent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.EnterAmountViewState
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountFatalError
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountInputError
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterAmount(
    viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
    navContextProvider: () -> NavContext,
    onBackPressed: () -> Unit
) {
    val viewState: EnterAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(viewModel) {
        analytics.logEvent(SwapAnalyticsEvents.EnterAmountViewed)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is EnterAmountNavigationEvent.Preview -> {
                    navContextProvider().navigateTo(
                        SwapGraph.Confirmation,
                        SwapConfirmationArgs(
                            sourceAccount = Bindable(event.sourceAccount),
                            targetAccount = Bindable(event.targetAccount),
                            sourceCryptoAmount = event.sourceCryptoAmount,
                            secondPassword = event.secondPassword,
                        )
                    )
                }
            }
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val localFocusManager = LocalFocusManager.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> keyboardController?.show()
                Lifecycle.Event.ON_PAUSE -> keyboardController?.hide()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(viewState.snackbarError) {
            val error = viewState.snackbarError
            if (error != null) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.localizedMessage ?: context.getString(
                        R.string.common_error
                    ),
                    duration = SnackbarDuration.Long,
                )
                viewModel.onIntent(EnterAmountIntent.SnackbarErrorHandled)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(color = AppTheme.colors.backgroundMuted)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            keyboardController?.hide()
                            localFocusManager.clearFocus()
                        }
                    )
                }
        ) {
            NavigationBar(
                title = stringResource(R.string.common_swap),
                onBackButtonClick = onBackPressed,
            )

            when (viewState.fatalError) {
                SwapEnterAmountFatalError.WalletLoading -> {
                    CustomEmptyState(
                        icon = Icons.Network.id,
                        ctaAction = { }
                    )
                }

                null -> {
                    EnterAmountScreen(
                        selected = viewState.selectedInput,
                        assets = viewState.assets,
                        maxAmount = viewState.maxAmount,
                        fiatAmount = viewState.fiatAmount,
                        onFiatAmountChanged = {
                            viewModel.onIntent(EnterAmountIntent.FiatInputChanged(it))
                        },
                        cryptoAmount = viewState.cryptoAmount,
                        onCryptoAmountChanged = {
                            viewModel.onIntent(EnterAmountIntent.CryptoInputChanged(it))
                        },
                        onFlipInputs = {
                            viewModel.onIntent(EnterAmountIntent.FlipInputs)
                        },
                        inputError = viewState.inputError,
                        inputErrorClicked = { error ->
                            navContextProvider().navigateTo(SwapGraph.InputError, error)
                        },
                        openSourceAccounts = {
                            navContextProvider().navigateTo(SwapGraph.SourceAccounts)
                            keyboardController?.hide()
                            analytics.logEvent(SwapAnalyticsEvents.SelectSourceClicked)
                        },
                        openTargetAccounts = { sourceTicker ->
                            navContextProvider().navigateTo(SwapGraph.TargetAsset, sourceTicker)
                            keyboardController?.hide()
                            analytics.logEvent(SwapAnalyticsEvents.SelectDestinationClicked)
                        },
                        setMaxOnClick = {
                            viewModel.onIntent(EnterAmountIntent.MaxSelected)
                            analytics.logEvent(SwapAnalyticsEvents.MaxClicked)
                        },
                        previewClicked = {
                            viewModel.onIntent(EnterAmountIntent.PreviewClicked)
                            analytics.logEvent(SwapAnalyticsEvents.PreviewClicked)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterAmountScreen(
    selected: InputCurrency,
    assets: EnterAmountAssets?,
    maxAmount: String?,
    fiatAmount: CurrencyValue?,
    onFiatAmountChanged: (String) -> Unit,
    cryptoAmount: CurrencyValue?,
    onCryptoAmountChanged: (String) -> Unit,
    onFlipInputs: () -> Unit,
    inputError: SwapEnterAmountInputError?,
    inputErrorClicked: (SwapEnterAmountInputError) -> Unit,
    openSourceAccounts: () -> Unit,
    openTargetAccounts: (sourceTicker: String) -> Unit,
    setMaxOnClick: () -> Unit,
    previewClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        safeLet(
            fiatAmount,
            cryptoAmount,
        ) { fiatAmount, cryptoAmount ->
            TwoCurrenciesInput(
                selected = selected,
                currency1 = fiatAmount,
                onCurrency1ValueChange = onFiatAmountChanged,
                currency2 = cryptoAmount,
                onCurrency2ValueChange = onCryptoAmountChanged,
                onFlipInputs = onFlipInputs,
            )
        }

        maxAmount?.let {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

            SmallTertiaryButton(
                modifier = Modifier.widthIn(min = 130.dp),
                text = stringResource(R.string.common_max_arg, maxAmount),
                onClick = setMaxOnClick,
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        assets?.let {
            TwoAssetActionHorizontal(
                startTitle = stringResource(R.string.common_from),
                start = HorizontalAssetAction(
                    assets.from.ticker,
                    StackedIcon.SingleIcon(ImageResource.Remote(assets.from.iconUrl)),
                ),
                startOnClick = openSourceAccounts,
                endTitle = stringResource(R.string.common_to),
                end = assets.to?.let {
                    HorizontalAssetAction(
                        it.ticker,
                        StackedIcon.SingleIcon(ImageResource.Remote(it.iconUrl)),
                    )
                },
                endOnClick = { openTargetAccounts(assets.from.ticker) }
            )
        } ?: TwoAssetActionHorizontalLoading()

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        inputError?.let {
            AlertButton(
                modifier = Modifier.fillMaxWidth(),
                text = when (inputError) {
                    is SwapEnterAmountInputError.BelowMinimum -> {
                        stringResource(R.string.minimum_with_value, inputError.minValue)
                    }

                    is SwapEnterAmountInputError.AboveMaximum -> {
                        stringResource(R.string.maximum_with_value, inputError.maxValue)
                    }

                    is SwapEnterAmountInputError.AboveBalance -> {
                        stringResource(
                            R.string.not_enough_funds, assets?.from?.ticker.orEmpty()
                        )
                    }

                    is SwapEnterAmountInputError.InsufficientGas ->
                        stringResource(
                            R.string.confirm_status_msg_insufficient_gas,
                            inputError.displayTicker
                        )

                    is SwapEnterAmountInputError.Unknown ->
                        stringResource(R.string.common_error)
                },
                state = ButtonState.Enabled,
                onClick = { inputErrorClicked(inputError) }
            )
        } ?: PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.preview_swap),
            state = if (fiatAmount?.isEmpty() == false && cryptoAmount?.isEmpty() == false) {
                ButtonState.Enabled
            } else {
                ButtonState.Disabled
            },
            onClick = {
                previewClicked()
            }
        )

        Spacer(modifier = Modifier.weight(4F))
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
private fun PreviewEnterAmountScreen() {
    EnterAmountScreen(
        selected = InputCurrency.Currency1,
        assets = EnterAmountAssets(
            from = EnterAmountAssetState(
                iconUrl = "",
                ticker = "BTC"
            ),
            to = EnterAmountAssetState(
                iconUrl = "",
                ticker = "ETH"
            )
        ),
        maxAmount = "123.00",
        fiatAmount = CurrencyValue(
            value = "2,100.00",
            maxFractionDigits = 2,
            ticker = "$",
            isPrefix = true,
            separateWithSpace = false,
            zeroHint = "0"

        ),
        onFiatAmountChanged = {},
        cryptoAmount = CurrencyValue(
            value = "1.1292",
            maxFractionDigits = 8,
            ticker = "ETH",
            isPrefix = false,
            separateWithSpace = true,
            zeroHint = "0"

        ),
        onCryptoAmountChanged = {},
        onFlipInputs = {},
        inputError = SwapEnterAmountInputError.BelowMinimum("éjdzjjdz"),
        inputErrorClicked = {},
        openSourceAccounts = {},
        openTargetAccounts = {},
        setMaxOnClick = {},
        previewClicked = {},
    )
}
