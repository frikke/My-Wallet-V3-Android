package com.blockchain.transactions.swap.enteramount.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountInputError

@Composable
fun InputErrorScreen(
    inputError: SwapEnterAmountInputError,
    closeClicked: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        SheetHeader(onClosePress = closeClicked, shouldShowDivider = false)

        val title = when (inputError) {
            is SwapEnterAmountInputError.AboveBalance ->
                stringResource(com.blockchain.stringResources.R.string.not_enough_funds, inputError.displayTicker)

            is SwapEnterAmountInputError.AboveMaximum ->
                stringResource(com.blockchain.stringResources.R.string.maximum_with_value, inputError.maxValue)

            is SwapEnterAmountInputError.BelowMinimum ->
                stringResource(com.blockchain.stringResources.R.string.minimum_with_value, inputError.minValue)

            is SwapEnterAmountInputError.InsufficientGas ->
                stringResource(com.blockchain.stringResources.R.string.not_enough_funds, inputError.displayTicker)

            is SwapEnterAmountInputError.Unknown ->
                stringResource(com.blockchain.stringResources.R.string.common_error)
        }

        val description = when (inputError) {
            is SwapEnterAmountInputError.AboveBalance -> stringResource(
                com.blockchain.stringResources.R.string.common_actions_not_enough_funds,
                inputError.displayTicker,
                "swap",
                inputError.balance,
            )

            is SwapEnterAmountInputError.AboveMaximum ->
                stringResource(com.blockchain.stringResources.R.string.trading_amount_above_max, inputError.maxValue)

            is SwapEnterAmountInputError.BelowMinimum ->
                stringResource(com.blockchain.stringResources.R.string.minimum_swap_error_message, inputError.minValue)

            is SwapEnterAmountInputError.InsufficientGas ->
                stringResource(
                    com.blockchain.stringResources.R.string.confirm_status_msg_insufficient_gas,
                    inputError.displayTicker
                )

            is SwapEnterAmountInputError.Unknown ->
                inputError.error ?: stringResource(com.blockchain.stringResources.R.string.common_error)
        }

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing),
            text = title,
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
        )
        SmallestVerticalSpacer()
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing),
            text = description,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
        )
        SmallVerticalSpacer()
    }
}
