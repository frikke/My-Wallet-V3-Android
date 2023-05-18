package com.dex.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.anim.AnimatedAmountCounter
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.dex.presentation.R
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

@Composable
fun SendAndReceiveAmountFields(
    modifier: Modifier = Modifier,
    onValueChanged: (TextFieldValue) -> Unit,
    sendAmountFieldConfig: AmountFieldConfig,
    receiveAmountFieldConfig: AmountFieldConfig
) {
    var input by remember {
        mutableStateOf(
            TextFieldValue(
                sendAmountFieldConfig.amount?.takeIf {
                    it.isPositive
                }?.toStringWithoutSymbol() ?: ""
            )
        )
    }

    /**
     * ensure that model amount and ui amount are in sync
     */
    if (sendAmountFieldConfig.amount == null &&
        input.text.toBigDecimalOrNull()?.signum() == 1
    ) {
        input = TextFieldValue("")
    }

    Box(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing)
                    )
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                Column {
                    AmountAndCurrencySelection(
                        isReadOnly = sendAmountFieldConfig.isReadOnly,
                        input = if (sendAmountFieldConfig.isReadOnly) {
                            TextFieldValue(
                                sendAmountFieldConfig.amount?.toStringWithoutSymbol().orEmpty()
                            )
                        } else {
                            input
                        },
                        onValueChanged = {
                            if (!sendAmountFieldConfig.isReadOnly &&
                                (it.text.isEmpty() || it.text.toDoubleOrNull() != null)
                            ) {
                                input = it
                                onValueChanged(it)
                            }
                        },
                        onClick = sendAmountFieldConfig.onCurrencyClicked,
                        currency = sendAmountFieldConfig.currency,
                        enabled = sendAmountFieldConfig.isEnabled,
                        canChangeCurrency = sendAmountFieldConfig.canChangeCurrency
                    )

                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sendAmountFieldConfig.exchange?.let {
                            ExchangeAmount(it, true)
                        }
                        when {
                            sendAmountFieldConfig.max != null -> MaxAmount(
                                maxAvailable = sendAmountFieldConfig.max,
                                maxClick = {
                                    val text = sendAmountFieldConfig.max.toStringWithoutSymbol()
                                    input = TextFieldValue(
                                        text = text,
                                        selection = TextRange(text.length)
                                    )
                                    onValueChanged(input)
                                }
                            )

                            sendAmountFieldConfig.balance != null -> BalanceAmount(sendAmountFieldConfig.balance)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            Row(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing)
                    )
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                Column {
                    ReceiveAmountAndCurrencySelection(
                        input = receiveAmountFieldConfig.amount?.toStringWithoutSymbol().orEmpty(),
                        onClick = receiveAmountFieldConfig.onCurrencyClicked,
                        currency = receiveAmountFieldConfig.currency,
                        enabled = receiveAmountFieldConfig.isEnabled,
                        animate = receiveAmountFieldConfig.shouldAnimateChanges,
                        canChangeCurrency = receiveAmountFieldConfig.canChangeCurrency
                    )

                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        receiveAmountFieldConfig.exchange?.let {
                            ExchangeAmount(
                                money = it,
                                isEnabled = receiveAmountFieldConfig.isEnabled,
                                shouldAnimateChanges = receiveAmountFieldConfig.shouldAnimateChanges
                            )
                        }
                        receiveAmountFieldConfig.balance?.takeIf { it.isPositive }?.let {
                            BalanceAmount(it)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(AppTheme.dimensions.hugeSpacing)
                .align(Alignment.Center),
            shape = CircleShape,
            border = BorderStroke(AppTheme.dimensions.tinySpacing, AppTheme.colors.backgroundMuted)
        ) {
            Image(
                imageResource = Icons.ArrowDown.withBackground(
                    backgroundColor = Color.White,
                    backgroundSize = AppTheme.dimensions.standardSpacing,
                    iconSize = AppTheme.dimensions.standardSpacing
                )
            )
        }
    }
}

@Composable
private fun ReceiveAmountAndCurrencySelection(
    input: String,
    enabled: Boolean,
    canChangeCurrency: Boolean,
    onClick: () -> Unit,
    currency: Currency?,
    animate: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = input.ifEmpty { "0" }
        val color = when {
            input.isEmpty() -> ComposeColors.Body
            !enabled -> ComposeColors.Dark
            else -> ComposeColors.Title
        }
        val modifier = Modifier.weight(1f)
        if (animate) {
            AnimatedAmountCounter(
                modifier = modifier,
                amountText = text,
                color = color,
                duration = 1000L,
                style = ComposeTypographies.Title2SlashedZero,
                gravity = ComposeGravities.Start
            )
        } else {
            SimpleText(
                modifier = modifier,
                text = text,
                style = ComposeTypographies.Title2SlashedZero,
                color = color,
                gravity = ComposeGravities.Start
            )
        }
        CurrencySelection(
            onClick = onClick,
            enabled = canChangeCurrency,
            currency = currency
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AmountAndCurrencySelection(
    isReadOnly: Boolean,
    input: TextFieldValue,
    enabled: Boolean,
    canChangeCurrency: Boolean,
    onClick: () -> Unit,
    currency: Currency?,
    onValueChanged: (TextFieldValue) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val interactionSource = remember { MutableInteractionSource() }

        BasicTextField(
            modifier = Modifier.weight(1f),
            value = input,
            singleLine = true,
            enabled = enabled,
            textStyle = AppTheme.typography.title2SlashedZero.copy(color = Grey900),
            readOnly = isReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = onValueChanged,
            maxLines = 1,
            interactionSource = interactionSource,
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = input.text,
                innerTextField = innerTextField,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(0.dp),
                enabled = enabled,
                placeholder = {
                    Text(
                        "0",
                        style = AppTheme.typography.title2SlashedZero,
                        color = Grey700
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Grey900,
                    disabledTextColor = Grey300,
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }

        CurrencySelection(
            onClick = onClick,
            enabled = canChangeCurrency,
            currency = currency
        )
    }
}

@Composable
private fun RowScope.ExchangeAmount(money: Money, isEnabled: Boolean, shouldAnimateChanges: Boolean = false) {
    val modifier = Modifier.weight(1f)
    val color = if (isEnabled) ComposeColors.Body else ComposeColors.Dark

    val style = ComposeTypographies.BodySlashedZero
    val gravity = ComposeGravities.Start
    if (shouldAnimateChanges) {
        AnimatedAmountCounter(
            modifier = modifier,
            amountText = money.toStringWithSymbol(),
            style = style,
            color = color,
            gravity = gravity
        )
    } else {
        SimpleText(
            modifier = modifier,
            text = money.toStringWithSymbol(),
            style = style,
            color = color,
            gravity = gravity
        )
    }
}

@Composable
private fun CurrencySelection(
    onClick: () -> Unit,
    enabled: Boolean,
    currency: Currency?
) {
    Row(
        modifier = Modifier
            .background(
                color = Grey000,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .clickable(enabled = enabled) {
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
                bottom = AppTheme.dimensions.smallestSpacing
            ),
            text = currency?.displayTicker ?: stringResource(
                id = com.blockchain.stringResources.R.string.common_select
            ),
            style = AppTheme.typography.body1,
            color = Grey900
        )
        if (enabled) {
            Image(
                ImageResource.Local(
                    id = com.blockchain.componentlib.R.drawable.ic_chevron_end,
                    colorFilter = ColorFilter.tint(Grey700),
                    size = 10.dp
                )
            )
        }
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
                    color = BackgroundMuted
                )
            }
        )
        Image(
            imageResource = Icons.ArrowDown.withBackground(
                backgroundColor = Color.White,
                backgroundSize = AppTheme.dimensions.standardSpacing,
                iconSize = AppTheme.dimensions.standardSpacing
            )
        )
    }
}

@Composable
private fun RowScope.MaxAmount(maxAvailable: Money, maxClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickableNoEffect { maxClick() }
            .wrapContentSize()
    ) {
        Text(
            text = stringResource(id = com.blockchain.stringResources.R.string.common_max),
            style = AppTheme.typography.micro2,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
        Text(
            text = maxAvailable.toStringWithSymbol(),
            style = AppTheme.typography.micro2SlashedZero,
            color = Blue600
        )
    }
}

@Composable
private fun BalanceAmount(amount: Money) {
    Row(
        modifier = Modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = com.blockchain.stringResources.R.string.common_balance),
            style = AppTheme.typography.micro2,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
        Text(
            text = amount.toStringWithSymbol(),
            style = AppTheme.typography.micro2SlashedZero,
            color = AppTheme.colors.title
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
            text = stringResource(id = com.blockchain.stringResources.R.string.common_balance),
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

@Preview
@Composable
private fun PreviewReceiveAmountAndCurrencySelection() {
    ReceiveAmountAndCurrencySelection(
        input = "123",
        enabled = false,
        canChangeCurrency = true,
        onClick = {},
        currency = CryptoCurrency.BTC,
        animate = true
    )
}

@Preview
@Composable
private fun PreviewAmountAndCurrencySelection() {
    AmountAndCurrencySelection(
        isReadOnly = false,
        input = TextFieldValue(text = "123"),
        enabled = true,
        canChangeCurrency = true,
        onClick = {},
        currency = CryptoCurrency.BTC,
        onValueChanged = {}
    )
}

@Preview
@Composable
private fun PreviewBalanceAmount() {
    BalanceAmount(Money.fromMajor(CryptoCurrency.BTC, 1234.toBigDecimal()))
}

@Preview
@Composable
private fun PreviewSendAndReceiveAmountFields() {
    SendAndReceiveAmountFields(
        onValueChanged = {},
        sendAmountFieldConfig = AmountFieldConfig(
            isEnabled = true, shouldAnimateChanges = false, isReadOnly = false, onCurrencyClicked = {},
            canChangeCurrency = true, amount = moneyPreview, exchange = moneyPreview, currency = CryptoCurrency.BTC,
            balance = moneyPreview, max = moneyPreview
        ),
        receiveAmountFieldConfig = AmountFieldConfig(
            isEnabled = true, shouldAnimateChanges = false, isReadOnly = false, onCurrencyClicked = {},
            canChangeCurrency = true, amount = moneyPreview, exchange = moneyPreview, currency = CryptoCurrency.BTC,
            balance = moneyPreview, max = moneyPreview
        ),
    )
}

private val moneyPreview = Money.fromMajor(CryptoCurrency.BTC, 1234.toBigDecimal())

class AmountFieldConfig(
    val isEnabled: Boolean,
    val shouldAnimateChanges: Boolean = false,
    val isReadOnly: Boolean,
    val onCurrencyClicked: () -> Unit,
    val canChangeCurrency: Boolean,
    val amount: Money?,
    val exchange: Money?,
    val currency: Currency?,
    val balance: Money?,
    val max: Money?
)
