package com.dex.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.DexPrefs
import com.dex.presentation.graph.DexDestination
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DexEnterAmountScreen(
    listState: LazyListState,
    navController: NavController,
    startReceiving: () -> Unit,
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
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(InputAmountIntent.InitTransaction)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                InputField(
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

@Preview
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
fun InputField(
    selectSourceAccount: () -> Unit,
    selectDestinationAccount: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit,
    viewState: InputAmountViewState.TransactionInputState
) {

    var input by remember { mutableStateOf(TextFieldValue()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Column {
                Row(
                    modifier = Modifier
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                        )
                        .onGloballyPositioned { coordinates ->
                            size = coordinates.size
                        }
                ) {
                    Column {
                        AmountAndCurrencySelection(
                            isReadOnly = false,
                            input = input,
                            onValueChanged = {
                                if (it.text.isEmpty() || it.text.toDoubleOrNull() != null) {
                                    input = it
                                    onValueChanged(it)
                                }
                            },
                            onClick = selectSourceAccount,
                            currency = viewState.sourceCurrency
                        )
                        Row {
                            viewState.inputExchangeAmount?.let {
                                ExchangeAmount(it)
                            }
                            viewState.maxAmount?.let {
                                MaxAmount(it)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                Row(
                    modifier = Modifier
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                        )
                ) {
                    Column {
                        AmountAndCurrencySelection(
                            isReadOnly = true,
                            input = TextFieldValue(viewState.outputAmount?.toStringWithoutSymbol().orEmpty()),
                            onValueChanged = {},
                            onClick = selectDestinationAccount,
                            currency = viewState.destinationCurrency
                        )
                        Row {
                            viewState.outputExchangeAmount?.let {
                                ExchangeAmount(it)
                            }
                            viewState.destinationAccountBalance?.let {
                                MaxAmount(it)
                            }
                        }
                    }
                }
            }
            MaskedCircleArrow(size)
        }

        if (viewState.error != DexUiError.None) {
            AlertButton(
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.small_spacing)),
                text = viewState.error.message(),
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Composable
private fun DexUiError.message(): String =
    when (this) {
        DexUiError.None -> throw IllegalStateException("No error message")
        is DexUiError.InsufficientFunds -> stringResource(id = R.string.not_enough_funds, currency.displayTicker)
    }

@Composable
private fun RowScope.ExchangeAmount(money: Money) {
    Text(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .weight(1f),
        text = money.toStringWithSymbol(),
        style = AppTheme.typography.bodyMono,
        color = Grey700
    )
}

@Composable
private fun RowScope.MaxAmount(maxAvailable: Money) {
    Row(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .wrapContentSize()
    ) {
        Text(
            text = stringResource(id = R.string.common_max),
            style = AppTheme.typography.micro2,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        Text(
            text = maxAvailable.toStringWithSymbol(),
            style = AppTheme.typography.micro2,
            color = Blue600
        )
    }
}

@Composable
private fun RowScope.Balance() {
    Row(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .wrapContentSize()
    ) {
        Text(
            text = stringResource(id = R.string.common_balance),
            style = AppTheme.typography.micro2,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        Text(
            text = "",
            style = AppTheme.typography.micro2,
            color = Grey900
        )
    }
}

@Composable
private fun MaskedCircleArrow(parentSize: IntSize) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                boxSize = coordinates.size
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = (parentSize.width / 2).minus(boxSize.width / 2),
                        y = parentSize.height.minus(boxSize.height / 2)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(dimensionResource(id = com.blockchain.componentlib.R.dimen.xlarge_spacing)),
            onDraw = {
                drawCircle(
                    color = BackgroundMuted,
                )
            }
        )
        Image(
            imageResource = Icons.ArrowDown.withBackground(
                backgroundColor = Color.White,
                backgroundSize = AppTheme.dimensions.standardSpacing,
                iconSize = AppTheme.dimensions.standardSpacing,
            )
        )
    }
}

@Composable
private fun CurrencySelection(onClick: () -> Unit, currency: Currency?) {
    Row(
        modifier = Modifier
            .background(
                color = Grey000,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .clickable {
                onClick()
            }
            .wrapContentSize()
            .padding(end = AppTheme.dimensions.tinySpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageResource = currency?.let {
                ImageResource.Remote(
                    url = it.logo,
                    size = AppTheme.dimensions.smallSpacing
                )
            } ?: ImageResource.Local(
                id = R.drawable.icon_no_account_selection,
                size = AppTheme.dimensions.smallSpacing
            ),
            modifier = Modifier.padding(start = AppTheme.dimensions.tinySpacing)
        )
        Text(
            modifier = Modifier.padding(
                start = AppTheme.dimensions.tinySpacing,
                end = AppTheme.dimensions.tinySpacing,
                top = AppTheme.dimensions.smallestSpacing,
                bottom = AppTheme.dimensions.smallestSpacing,
            ),
            text = currency?.let {
                it.displayTicker
            } ?: stringResource(id = R.string.common_select),
            style = AppTheme.typography.body1,
            color = Grey900
        )
        Image(
            ImageResource.Local(
                id = R.drawable.ic_chevron_end,
                colorFilter = ColorFilter.tint(Grey700),
                size = 10.dp
            )
        )
    }
}

@Composable
private fun AmountAndCurrencySelection(
    isReadOnly: Boolean,
    input: TextFieldValue,
    currency: Currency?,
    onValueChanged: (TextFieldValue) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                end = AppTheme.dimensions.smallSpacing,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = input,
            textStyle = AppTheme.typography.title2Mono,
            readOnly = isReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = {
                Text(
                    "0",
                    style = AppTheme.typography.title2Mono,
                    color = Grey700
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                textColor = Grey900,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            maxLines = 1,
            onValueChange = onValueChanged
        )
        CurrencySelection(onClick = onClick, currency = currency)
    }
}
