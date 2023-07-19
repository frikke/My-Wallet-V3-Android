package com.dex.presentation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.anim.AnimatedAmountCounter
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.dex.presentation.R
import com.blockchain.utils.removeLeadingZeros
import com.blockchain.utils.stripThousandSeparators
import com.blockchain.utils.toBigDecimalOrNullFromLocalisedInput
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.text.DecimalFormatSymbols

@Composable
fun SendAndReceiveAmountFields(
    modifier: Modifier = Modifier,
    reset: Boolean,
    sendAmountFieldConfig: AmountFieldConfig,
    receiveAmountFieldConfig: AmountFieldConfig
) {

    var lastInputField by remember { mutableIntStateOf(-1) }
    var applyMax by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = sendAmountFieldConfig, block = {
        applyMax = false
    })

    Box(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier
                    .background(
                        color = AppColors.backgroundSecondary,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing)
                    )
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                Column {
                    if (sendAmountFieldConfig.isReadOnly) {
                        ReadOnlyAmount(
                            sendAmountFieldConfig
                        )
                    } else {
                        AmountAndCurrencySelection(
                            fieldConfig = sendAmountFieldConfig,
                            isActiveTyping = lastInputField == 0,
                            internalValueChange = { lastInputField = 0 },
                            reset = reset,
                            applyMax = applyMax
                        )
                    }
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sendAmountFieldConfig.exchange?.let {
                            ExchangeAmount(it, sendAmountFieldConfig.isEnabled)
                        }
                        when {
                            sendAmountFieldConfig.max != null -> MaxAmount(
                                maxAvailable = sendAmountFieldConfig.max,
                                maxClick = {
                                    applyMax = true
                                    lastInputField = 0
                                    sendAmountFieldConfig.onValueChanged(
                                        sendAmountFieldConfig.max.toStringWithoutSymbol().stripThousandSeparators()
                                    )
                                }
                            )

                            sendAmountFieldConfig.balance != null -> BalanceAmount(
                                sendAmountFieldConfig.balance
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            Row(
                modifier = Modifier
                    .background(
                        color = AppColors.backgroundSecondary,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing)
                    )
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                Column {
                    if (receiveAmountFieldConfig.isReadOnly) {
                        ReadOnlyAmount(
                            receiveAmountFieldConfig
                        )
                    } else {
                        AmountAndCurrencySelection(
                            fieldConfig = receiveAmountFieldConfig,
                            isActiveTyping = lastInputField == 1,
                            reset = reset,
                            applyMax = false,
                            internalValueChange = { lastInputField = 1 },
                        )
                    }
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
            border = BorderStroke(AppTheme.dimensions.tinySpacing, AppTheme.colors.background)
        ) {
            Image(
                imageResource = Icons.ArrowDown
                    .withTint(AppColors.title)
                    .withBackground(
                        backgroundColor = AppColors.backgroundSecondary,
                        backgroundSize = AppTheme.dimensions.standardSpacing,
                        iconSize = AppTheme.dimensions.standardSpacing
                    )
            )
        }
    }
}

private fun String.isValidDecimalNumber(): Boolean {

    val decimalRegex =
        """^-?\d*${Regex.escape(DecimalFormatSymbols.getInstance().decimalSeparator.toString())}?\d*$""".toRegex()
    return matches(decimalRegex) && toBigDecimalOrNullFromLocalisedInput() != null
}

@Composable
private fun ReadOnlyAmount(
    config: AmountFieldConfig
) {
    val input = config.amount?.toStringWithoutSymbol().orEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = input.ifEmpty { "0" }
        val color = when {
            input.isEmpty() -> ComposeColors.Body
            !config.isEnabled -> ComposeColors.Dark
            else -> ComposeColors.Title
        }
        val modifier = Modifier.weight(1f)
        if (config.shouldAnimateChanges) {
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
            onClick = config.onCurrencyClicked,
            enabled = config.isEnabled,
            currency = config.currency
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AmountAndCurrencySelection(
    fieldConfig: AmountFieldConfig,
    isActiveTyping: Boolean,
    reset: Boolean,
    applyMax: Boolean,
    internalValueChange: () -> Unit,
) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                fieldConfig.amount?.takeIf {
                    it.isPositive
                }?.toStringWithoutSymbol() ?: ""
            )
        )
    }

    LaunchedEffect(key1 = reset, block = {
        if (reset) {
            input = TextFieldValue()
        }
    })

    LaunchedEffect(key1 = applyMax, block = {
        if (applyMax) {
            fieldConfig.max?.toStringWithoutSymbol()?.stripThousandSeparators()?.let {
                input = TextFieldValue(
                    it
                )
            }
        }
    })

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val interactionSource = remember { MutableInteractionSource() }

        BasicTextField(
            modifier = Modifier.weight(1f),
            value = if (!isActiveTyping) {
                TextFieldValue(
                    fieldConfig.amount?.toStringWithoutSymbol().orEmpty()
                ).also {
                    input = it
                }
            } else {
                input.copy(
                    text = input.text.stripThousandSeparators()
                )
            },
            singleLine = true,

            textStyle = AppTheme.typography.title2SlashedZero.copy(
                color = if (fieldConfig.isEnabled) AppColors.title else AppColors.dark
            ),
            enabled = fieldConfig.isEnabled,
            readOnly = fieldConfig.isReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            onValueChange = {
                if ((it.text.isEmpty() || it.text.stripThousandSeparators().isValidDecimalNumber())) {
                    if (input.text != it.text) {
                        fieldConfig.onValueChanged(
                            it.text.takeIf { text -> text.isNotEmpty() }
                                ?.toBigDecimalOrNullFromLocalisedInput()?.toString() ?: ""
                        )
                    }
                    internalValueChange()
                    input = it.copy(text = it.text.removeLeadingZeros())
                }
            },
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
                enabled = fieldConfig.isEnabled,
                placeholder = {
                    Text(
                        "0",
                        style = AppTheme.typography.title2SlashedZero,
                        color = AppColors.body
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = AppColors.primary,
                    disabledTextColor = AppColors.primary,
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }
        CurrencySelection(
            onClick = fieldConfig.onCurrencyClicked,
            enabled = fieldConfig.canChangeCurrency,
            currency = fieldConfig.currency
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
    val hasCurrencySelected = currency != null
    Row(
        modifier = Modifier
            .background(
                color = if (hasCurrencySelected) AppTheme.colors.light else AppTheme.colors.primary,
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
            ).withTint(AppTheme.colors.backgroundSecondary),
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
            color = if (hasCurrencySelected) AppTheme.colors.title else AppTheme.colors.backgroundSecondary
        )
        if (enabled) {
            Image(
                Icons.ChevronRight.withTint(
                    if (hasCurrencySelected)
                        AppTheme.colors.body
                    else
                        AppTheme.colors.backgroundSecondary
                ).withSize(10.dp)
            )
        }
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
            color = AppColors.body
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
        Text(
            text = maxAvailable.toStringWithSymbol(),
            style = AppTheme.typography.micro2SlashedZero,
            color = AppColors.primary
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
            color = AppColors.body
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
            color = AppColors.body
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        Text(
            text = "",
            style = AppTheme.typography.micro2,
            color = AppColors.title
        )
    }
}

@Preview
@Composable
private fun PreviewBalanceAmount() {
    BalanceAmount(Money.fromMajor(CryptoCurrency.BTC, 1234.toBigDecimal()))
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBalanceAmountDark() {
    PreviewBalanceAmount()
}

@Preview
@Composable
private fun PreviewSendAndReceiveAmountFields() {
    SendAndReceiveAmountFields(
        sendAmountFieldConfig = AmountFieldConfig(
            isEnabled = true, shouldAnimateChanges = false, isReadOnly = false, onCurrencyClicked = {},
            canChangeCurrency = true, amount = moneyPreview, exchange = moneyPreview, currency = CryptoCurrency.BTC,
            balance = moneyPreview, max = moneyPreview
        ),
        reset = false,
        receiveAmountFieldConfig = AmountFieldConfig(
            isEnabled = true, shouldAnimateChanges = false, isReadOnly = false, onCurrencyClicked = {},
            canChangeCurrency = true, amount = moneyPreview, exchange = moneyPreview, currency = CryptoCurrency.BTC,
            balance = moneyPreview, max = moneyPreview
        ),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSendAndReceiveAmountFieldsDark() {
    PreviewSendAndReceiveAmountFields()
}

private val moneyPreview = Money.fromMajor(CryptoCurrency.BTC, 1234.toBigDecimal())

class AmountFieldConfig(
    val isEnabled: Boolean,
    val shouldAnimateChanges: Boolean = false,
    val isReadOnly: Boolean,
    val onValueChanged: (String) -> Unit = {},
    val onCurrencyClicked: () -> Unit,
    val canChangeCurrency: Boolean,
    val amount: Money?,
    val exchange: Money?,
    val currency: Currency?,
    val balance: Money?,
    val max: Money?
)
