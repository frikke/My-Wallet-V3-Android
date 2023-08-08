package piuk.blockchain.android.simplebuy.sheets

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
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.launchUrlInBrowser

class AchWithdrawalHoldInfoBottomSheet : ComposeModalBottomDialog() {

    @Composable
    override fun Sheet() {
        AchWithdrawalHoldInfoSheet(
            onCloseClick = { dismiss() },
            onLearnMoreClick = { requireContext().launchUrlInBrowser(TRADING_ACCOUNT_LOCKS) }
        )
    }

    companion object {
        fun newInstance(): AchWithdrawalHoldInfoBottomSheet =
            AchWithdrawalHoldInfoBottomSheet()
    }
}

@Composable
fun AchWithdrawalHoldInfoSheet(
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit
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
            title = stringResource(id = com.blockchain.stringResources.R.string.withdrawal_holds),
            onClosePress = onCloseClick,
        )

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        SimpleText(
            modifier = Modifier
                .padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(com.blockchain.stringResources.R.string.deposit_terms_withdrawal_hold_info),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        MinimalPrimarySmallButton(
            modifier = Modifier
                .padding(
                    horizontal = AppTheme.dimensions.standardSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                ),
            onClick = onLearnMoreClick,
            text = stringResource(id = com.blockchain.stringResources.R.string.common_learn_more)
        )
    }
}

@Preview
@Composable
private fun AchWithdrawalHoldInfoSheetPreview() {
    AppTheme {
        AppSurface {
            AchWithdrawalHoldInfoSheet(
                onCloseClick = {},
                onLearnMoreClick = {}
            )
        }
    }
}
