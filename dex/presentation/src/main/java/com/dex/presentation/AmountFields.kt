package com.dex.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey200
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.dex.presentation.R
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

@Composable
fun SourceAndDestinationAmountFields(
    modifier: Modifier = Modifier,
    onValueChanged: (TextFieldValue) -> Unit,
    sourceAmountFieldConfig: AmountFieldConfig,
    destinationAmountFieldConfig: AmountFieldConfig,
) {
    var input by remember {
        mutableStateOf(
            TextFieldValue(
                sourceAmountFieldConfig.amount?.takeIf {
                    it.isPositive
                }?.toStringWithoutSymbol() ?: ""
            )
        )
    }

    if (sourceAmountFieldConfig.amount == null && input.text.isNotEmpty()) {
        input = TextFieldValue("")
    }

    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier) {
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
                        isReadOnly = sourceAmountFieldConfig.isReadOnly,
                        input = if (sourceAmountFieldConfig.isReadOnly) {
                            TextFieldValue(
                                sourceAmountFieldConfig.amount?.toStringWithoutSymbol().orEmpty()
                            )
                        } else input,
                        onValueChanged = {
                            if (!sourceAmountFieldConfig.isReadOnly &&
                                (it.text.isEmpty() || it.text.toDoubleOrNull() != null)
                            ) {
                                input = it
                                onValueChanged(it)
                            }
                        },
                        onClick = sourceAmountFieldConfig.onCurrencyClicked,
                        currency = sourceAmountFieldConfig.currency,
                        enabled = sourceAmountFieldConfig.isEnabled,
                        canChangeCurrency = sourceAmountFieldConfig.canChangeCurrency
                    )
                    Row {
                        sourceAmountFieldConfig.exchange?.let {
                            ExchangeAmount(it, true)
                        }
                        when {
                            sourceAmountFieldConfig.max != null -> MaxAmount(
                                maxAvailable = sourceAmountFieldConfig.max,
                                maxClick = {
                                    val text = sourceAmountFieldConfig.max.toStringWithoutSymbol()
                                    input = TextFieldValue(
                                        text = text,
                                        selection = TextRange(text.length)
                                    )
                                    onValueChanged(input)
                                }
                            )
                            sourceAmountFieldConfig.balance != null -> BalanceAmount(sourceAmountFieldConfig.balance)
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
                        isReadOnly = destinationAmountFieldConfig.isReadOnly,
                        input = TextFieldValue(destinationAmountFieldConfig.amount?.toStringWithoutSymbol().orEmpty()),
                        onValueChanged = {},
                        onClick = destinationAmountFieldConfig.onCurrencyClicked,
                        currency = destinationAmountFieldConfig.currency,
                        enabled = destinationAmountFieldConfig.isEnabled,
                        canChangeCurrency = destinationAmountFieldConfig.canChangeCurrency
                    )
                    Row {
                        destinationAmountFieldConfig.exchange?.let {
                            ExchangeAmount(
                                money = it,
                                isEnabled = destinationAmountFieldConfig.isEnabled
                            )
                        }
                        destinationAmountFieldConfig.balance?.takeIf { it.isPositive }?.let {
                            BalanceAmount(it)
                        }
                    }
                }
            }
        }
        MaskedCircleArrow(size)
    }
}

@Composable
private fun AmountAndCurrencySelection(
    isReadOnly: Boolean,
    input: TextFieldValue,
    enabled: Boolean,
    canChangeCurrency: Boolean,
    onClick: () -> Unit,
    currency: Currency?,
    onValueChanged: (TextFieldValue) -> Unit,
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
            singleLine = true,
            enabled = enabled,
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
                disabledTextColor = Grey300,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            maxLines = 1,
            onValueChange = onValueChanged
        )
        CurrencySelection(
            onClick = onClick,
            enabled = canChangeCurrency,
            currency = currency,
        )
    }
}

@Composable
private fun RowScope.ExchangeAmount(money: Money, isEnabled: Boolean) {
    Text(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .weight(1f),
        text = money.toStringWithSymbol(),
        style = AppTheme.typography.bodyMono,
        color = if (isEnabled) Grey700 else Grey200
    )
}

@Composable
private fun CurrencySelection(
    onClick: () -> Unit,
    enabled: Boolean,
    currency: Currency?,
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
                bottom = AppTheme.dimensions.smallestSpacing,
            ),
            text = currency?.displayTicker ?: stringResource(id = R.string.common_select),
            style = AppTheme.typography.body1,
            color = Grey900
        )
        if (enabled)
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
private fun RowScope.MaxAmount(maxAvailable: Money, maxClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .clickableNoEffect { maxClick() }
            .wrapContentSize()
    ) {
        Text(
            text = stringResource(id = R.string.common_max),
            style = AppTheme.typography.micro2,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
        Text(
            text = maxAvailable.toStringWithSymbol(),
            style = AppTheme.typography.micro2,
            color = Blue600
        )
    }
}

@Composable
private fun BalanceAmount(amount: Money) {
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
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
        Text(
            text = amount.toStringWithSymbol(),
            style = AppTheme.typography.micro2,
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

class AmountFieldConfig(
    val isEnabled: Boolean,
    val isReadOnly: Boolean,
    val onCurrencyClicked: () -> Unit,
    val canChangeCurrency: Boolean,
    val amount: Money?,
    val exchange: Money?,
    val currency: Currency?,
    val balance: Money?,
    val max: Money?
)
