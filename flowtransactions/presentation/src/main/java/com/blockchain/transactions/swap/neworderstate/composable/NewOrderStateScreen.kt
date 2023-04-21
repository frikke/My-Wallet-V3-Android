package com.blockchain.transactions.swap.neworderstate.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.White
import com.blockchain.transactions.presentation.R
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import java.io.Serializable

enum class NewOrderState {
    PENDING_DEPOSIT,
    SUCCEEDED,
}

data class NewOrderStateArgs(
    val sourceAmount: CryptoValue,
    val targetAmount: CryptoValue,
    val orderState: NewOrderState,
) : Serializable

@Composable
fun NewOrderStateScreen(
    args: NewOrderStateArgs,
    exitSwap: () -> Unit,
) {
    Column {
        Spacer(Modifier.weight(0.33f))

        Column {
            val swapIcon = Icons.Swap
                .withTint(AppTheme.colors.title)
                .withBackground(
                    backgroundColor = White,
                    iconSize = 59.dp,
                    backgroundSize = 88.dp
                )
            val tagIcon = when (args.orderState) {
                NewOrderState.PENDING_DEPOSIT ->
                    Icons.Filled.Pending
                        .withTint(AppTheme.colors.muted)
                        .withSize(44.dp)
                NewOrderState.SUCCEEDED ->
                    Icons.Check
                        .withTint(White)
                        .withBackground(
                            backgroundColor = AppTheme.colors.success,
                            iconSize = 44.dp,
                            backgroundSize = 44.dp
                        )
            }

            val stackedIcon = StackedIcon.SmallTag(
                main = swapIcon,
                tag = tagIcon,
            )

            SmallTagIcon(
                icon = stackedIcon,
                iconBackground = White,
                mainIconSize = 88.dp,
                tagIconSize = 44.dp,
            )

            SmallVerticalSpacer()

            val title = when (args.orderState) {
                NewOrderState.PENDING_DEPOSIT ->
                    stringResource(R.string.swap_neworderstate_pending_deposit_title, args.sourceAmount.currency.name)
                NewOrderState.SUCCEEDED ->
                    stringResource(R.string.swap_neworderstate_succeeded_title)
            }
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                text = title,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
            )

            TinyHorizontalSpacer()

            val description = when (args.orderState) {
                NewOrderState.PENDING_DEPOSIT -> stringResource(
                    R.string.swap_neworderstate_pending_deposit_description,
                    args.sourceAmount.toStringWithSymbol(),
                    args.targetAmount.toStringWithSymbol(),
                    // TODO(aromano): usually X minutes
                    "5"
                )
                NewOrderState.SUCCEEDED -> stringResource(
                    R.string.swap_neworderstate_succeeded_description,
                    args.sourceAmount.toStringWithSymbol(),
                    args.targetAmount.toStringWithSymbol(),
                )
            }
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                text = description,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
            )
        }

        Spacer(Modifier.weight(0.66f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing),
            text = stringResource(R.string.common_done),
            onClick = exitSwap,
        )
    }
}

@Preview
@Composable
private fun PreviewPendingDeposit() {
    NewOrderStateScreen(
        args = NewOrderStateArgs(
            sourceAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.5.toBigDecimal()),
            targetAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.05.toBigDecimal()),
            orderState = NewOrderState.PENDING_DEPOSIT,
        ),
        exitSwap = {},
    )
}

@Preview
@Composable
private fun PreviewSucceeded() {
    NewOrderStateScreen(
        args = NewOrderStateArgs(
            sourceAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.5.toBigDecimal()),
            targetAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.05.toBigDecimal()),
            orderState = NewOrderState.SUCCEEDED,
        ),
        exitSwap = {},
    )
}