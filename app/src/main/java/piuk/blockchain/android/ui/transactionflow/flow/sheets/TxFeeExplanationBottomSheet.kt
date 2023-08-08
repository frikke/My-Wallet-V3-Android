package piuk.blockchain.android.ui.transactionflow.flow.sheets

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tag.SuccessTag
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R

class TxFeeExplanationBottomSheet : ComposeModalBottomDialog() {

    private val sheetTitle by lazy {
        arguments?.getString(TX_FEE_TITLE).orEmpty()
    }

    private val displayTicker by lazy {
        arguments?.getString(TX_FEE_TICKER).orEmpty()
    }

    private val availableLabel by lazy {
        arguments?.getString(TX_FEE_AVAILABLE_LABEL).orEmpty()
    }

    private val totalBalance by lazy {
        arguments?.getSerializable(TX_FEE_TOTAL) as Money
    }

    private val estimatedFee by lazy {
        arguments?.getSerializable(TX_FEE_ESTIMATE) as Money
    }

    private val availableBalance by lazy {
        arguments?.getSerializable(TX_FEE_AVAILABLE) as Money
    }

    private val isTransactionFree by lazy {
        arguments?.getBoolean(TX_FEE_IS_FREE) ?: false
    }

    @Composable
    override fun Sheet() {
        TransactionFeeExplanationSheet(
            title = sheetTitle,
            displayTicker = displayTicker,
            availableLabel = availableLabel,
            totalBalance = totalBalance,
            estimatedFee = estimatedFee,
            availableBalance = availableBalance,
            onCloseClick = {
                this.dismiss()
            },
            isTransactionFree = isTransactionFree
        )
    }

    companion object {
        private const val TX_FEE_TITLE = "TX_FEE_TITLE"
        private const val TX_FEE_TICKER = "TX_FEE_TICKER"
        private const val TX_FEE_AVAILABLE_LABEL = "TX_FEE_AVAILABLE_LABEL"
        private const val TX_FEE_TOTAL = "TX_FEE_TOTAL"
        private const val TX_FEE_ESTIMATE = "TX_FEE_ESTIMATE"
        private const val TX_FEE_AVAILABLE = "TX_FEE_AVAILABLE"
        private const val TX_FEE_IS_FREE = "TX_FEE_IS_FREE"

        fun newInstance(
            title: String,
            displayTicker: String,
            availableLabel: String,
            totalBalance: Money,
            estimatedFee: Money,
            availableBalance: Money,
            isTransactionFree: Boolean
        ) = TxFeeExplanationBottomSheet().apply {
            arguments = Bundle().apply {
                putString(TX_FEE_TITLE, title)
                putString(TX_FEE_TICKER, displayTicker)
                putString(TX_FEE_AVAILABLE_LABEL, availableLabel)
                putSerializable(TX_FEE_TOTAL, totalBalance)
                putSerializable(TX_FEE_ESTIMATE, estimatedFee)
                putSerializable(TX_FEE_AVAILABLE, availableBalance)
                putBoolean(TX_FEE_IS_FREE, isTransactionFree)
            }
        }
    }
}

@Composable
fun TransactionFeeExplanationSheet(
    title: String,
    displayTicker: String,
    availableLabel: String,
    totalBalance: Money,
    estimatedFee: Money,
    availableBalance: Money,
    onCloseClick: () -> Unit,
    isTransactionFree: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.background,
                shape = RoundedCornerShape(
                    dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)
                )
            ),
        horizontalAlignment = Alignment.Start
    ) {
        SheetHeader(
            title = title,
            onClosePress = onCloseClick,
        )

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        SimpleText(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(com.blockchain.stringResources.R.string.tx_enter_amount_fee_sheet_description),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)))

        TransactionFeeRowItem(
            startingText = stringResource(
                id = com.blockchain.stringResources.R.string.tx_enter_amount_fee_sheet_total_label,
                displayTicker
            ),
            endingText = totalBalance.toStringWithSymbol(),
            showBottomDivider = true
        )

        TransactionFeeRowItem(
            startingText = stringResource(com.blockchain.stringResources.R.string.tx_enter_amount_fee_sheet_fee_label),
            endingText = estimatedFee.toStringWithSymbol(),
            showBottomDivider = true,
            showFreeBadge = isTransactionFree
        )

        TransactionFeeRowItem(
            startingText = availableLabel,
            endingText = availableBalance.toStringWithSymbol(),
            showBottomDivider = false
        )

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.standardSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(com.blockchain.stringResources.R.string.common_ok),
            onClick = onCloseClick
        )
    }
}

@Composable
fun TransactionFeeRowItem(
    startingText: String,
    endingText: String,
    showBottomDivider: Boolean,
    showFreeBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.standardSpacing)
    ) {
        SimpleText(
            modifier = Modifier.weight(weight = 1f),
            text = startingText,
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        if (showFreeBadge) {
            SuccessTag(
                text = stringResource(com.blockchain.stringResources.R.string.common_free),
                size = TagSize.Primary
            )
        } else {
            SimpleText(
                text = endingText,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.End
            )
        }
    }

    if (showBottomDivider) {
        Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        HorizontalDivider(dividerColor = AppTheme.colors.medium, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))
    }
}

@Preview
@Composable
fun FeeBottomSheet() {
    AppTheme {
        AppSurface {
            TransactionFeeExplanationSheet(
                title = "Swap fees explanation",
                displayTicker = "BTC",
                availableLabel = "Available to swap",
                totalBalance = Money.zero(CryptoCurrency.BTC),
                estimatedFee = Money.zero(CryptoCurrency.BTC),
                availableBalance = Money.zero(CryptoCurrency.BTC),
                onCloseClick = {},
                isTransactionFree = false
            )
        }
    }
}

@Preview
@Composable
fun FeeBottomSheet_Free() {
    AppTheme {
        AppSurface {
            TransactionFeeExplanationSheet(
                title = "Swap fees explanation",
                displayTicker = "BTC",
                availableLabel = "Available to swap",
                totalBalance = Money.zero(CryptoCurrency.BTC),
                estimatedFee = Money.zero(CryptoCurrency.BTC),
                availableBalance = Money.zero(CryptoCurrency.BTC),
                onCloseClick = {},
                isTransactionFree = true
            )
        }
    }
}
