package piuk.blockchain.android.simplebuy.sheets

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.utils.unsafeLazy
import piuk.blockchain.android.R

class AchTermsAndConditionsBottomSheet : ComposeModalBottomDialog() {

    private val bankLabel: String by unsafeLazy { arguments?.getString(BANK_LABEL_KEY) ?: "" }
    private val amount: String by unsafeLazy { arguments?.getString(AMOUNT_KEY) ?: "" }
    private val withdrawalLock: String by unsafeLazy { arguments?.getString(WITHDRAWAL_LOCK_KEY) ?: "" }
    private val isRecurringBuyEnabled: Boolean by unsafeLazy { arguments?.getBoolean(RECURRING_BUY_KEY) ?: false }

    @Composable
    override fun Sheet() {
        AchTermsAndConditionsSheet(
            bankLabel = bankLabel,
            amount = amount,
            withdrawalLock = withdrawalLock,
            isRecurringBuyEnabled = isRecurringBuyEnabled,
            onCloseClick = { dismiss() }
        )
    }

    companion object {
        private const val BANK_LABEL_KEY = "BANK_LABEL_KEY"
        private const val AMOUNT_KEY = "AMOUNT_KEY"
        private const val WITHDRAWAL_LOCK_KEY = "WITHDRAWAL_LOCK_KEY"
        private const val RECURRING_BUY_KEY = "RECURRING_BUY_KEY"

        fun newInstance(bankLabel: String, amount: String, withdrawalLock: String, isRecurringBuyEnabled: Boolean) =
            AchTermsAndConditionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(BANK_LABEL_KEY, bankLabel)
                    putString(AMOUNT_KEY, amount)
                    putString(WITHDRAWAL_LOCK_KEY, withdrawalLock)
                    putBoolean(RECURRING_BUY_KEY, isRecurringBuyEnabled)
                }
            }
    }
}

@Composable
fun AchTermsAndConditionsSheet(
    bankLabel: String,
    amount: String,
    withdrawalLock: String,
    isRecurringBuyEnabled: Boolean,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.light,
                shape = RoundedCornerShape(
                    dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)
                )
            ),
        horizontalAlignment = Alignment.Start
    ) {
        SheetHeader(
            title = stringResource(id = com.blockchain.stringResources.R.string.terms_and_conditions),
            onClosePress = onCloseClick,
        )

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        SimpleText(
            modifier = Modifier
                .padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(
                if (isRecurringBuyEnabled) {
                    com.blockchain.stringResources.R.string.checkout_terms_and_conditions_recurring
                } else {
                    com.blockchain.stringResources.R.string.checkout_terms_and_conditions
                },
                bankLabel,
                amount,
                withdrawalLock
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
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

@Preview
@Composable
private fun AchTermsAndConditionsSheetPreview() {
    AppTheme {
        AppSurface {
            AchTermsAndConditionsSheet(
                bankLabel = "CHASE 0000",
                amount = "$50.00",
                withdrawalLock = "7",
                isRecurringBuyEnabled = false,
                onCloseClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun AchTermsAndConditionsRecurringSheetPreview() {
    AppTheme {
        AppSurface {
            AchTermsAndConditionsSheet(
                bankLabel = "CHASE 0000",
                amount = "$50.00",
                withdrawalLock = "7",
                isRecurringBuyEnabled = true,
                onCloseClick = {}
            )
        }
    }
}
