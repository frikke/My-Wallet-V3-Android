package piuk.blockchain.android.ui.interest.tbm.presentation.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestDetail

@Composable
fun InterestDashboardAssetItem(assetInfo: AssetInfo, assetInterestDetail: AssetInterestDetail?, isKycGold: Boolean) {
    Column(modifier = Modifier.padding(dimensionResource(R.dimen.standard_margin))) {

        AssetName(assetInfo)

        if (assetInterestDetail != null) {
            Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

            InterestApy(assetInfo, assetInterestDetail)

            HorizontalDivider(
                modifier = Modifier
                    .padding(
                        top = dimensionResource(R.dimen.very_small_margin),
                        bottom = dimensionResource(R.dimen.very_small_margin)
                    )
                    .fillMaxWidth()
            )

            InterestBalance(assetInfo, assetInterestDetail)
        }

        Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

        InterestCta(
            ctaText = stringResource(
                when {
                    assetInterestDetail == null -> R.string.rewards_dashboard_item_action_earn
                    assetInterestDetail.totalBalance.isPositive -> R.string.rewards_dashboard_item_action_view
                    else -> R.string.rewards_dashboard_item_action_earn
                }
            ),
            enabled = (isKycGold && assetInterestDetail?.eligible == true) || assetInterestDetail?.totalBalance?.isPositive == true
        )

        if (assetInterestDetail == null || assetInterestDetail.ineligibilityReason != IneligibilityReason.NONE) {
            Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))
            InterestExplainer(
                stringResource(
                    when (assetInterestDetail?.ineligibilityReason) {
                        IneligibilityReason.REGION -> R.string.rewards_item_issue_region
                        IneligibilityReason.KYC_TIER -> R.string.rewards_item_issue_kyc
                        else -> R.string.rewards_item_issue_other
                    }
                )
            )
        }
    }
}

@Composable
private fun AssetName(assetInfo: AssetInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .size(dimensionResource(R.dimen.standard_margin))
                .clip(CircleShape),
            imageResource = ImageResource.Remote(assetInfo.logo)
        )

        Text(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
            style = AppTheme.typography.title3,
            text = assetInfo.name
        )
    }
}

@Composable
private fun InterestApy(assetInfo: AssetInfo, assetInterestDetail: AssetInterestDetail) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageResource = ImageResource.Local(id = R.drawable.ic_information)
        )

        Text(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            text = buildAnnotatedString {
                append(stringResource(id = R.string.rewards_dashboard_item_rate_1))

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${assetInterestDetail.rate}%")
                }

                append(stringResource(id = R.string.rewards_dashboard_item_rate_2, assetInfo.name))
            },
        )
    }
}

@Composable
private fun InterestCta(ctaText: String, enabled: Boolean) {
    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = ctaText,
        state = if (enabled) ButtonState.Enabled else ButtonState.Disabled,
        onClick = { },
    )
}

@Composable
private fun InterestBalance(assetInfo: AssetInfo, assetInterestDetail: AssetInterestDetail) {
    Row {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                style = AppTheme.typography.caption1,
                text = stringResource(id = R.string.rewards_dashboard_item_balance_title, assetInfo.displayTicker),
                color = Grey800
            )

            Text(
                style = AppTheme.typography.paragraph2,
                text = assetInterestDetail.totalBalance.toStringWithSymbol(),
                color = Grey800
            )
        }

        Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                style = AppTheme.typography.caption1,
                text = stringResource(id = R.string.rewards_dashboard_item_accrued_title),
                color = Grey800
            )

            Text(
                style = AppTheme.typography.paragraph2,
                text = assetInterestDetail.totalInterest.toStringWithSymbol(),
                color = Grey800
            )
        }
    }
}

@Composable
private fun InterestExplainer(explanation: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageResource = ImageResource.Local(id = R.drawable.ic_information)
        )

        Text(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.minuscule_margin)),
            style = AppTheme.typography.paragraph1,
            text = explanation,
            color = Grey800
        )
    }
}

@Preview
@Composable
private fun PreviewAssetInterestItemError() {
    InterestDashboardAssetItem(
        CryptoCurrency.BTC,
        null,
        true
    )
}

@Preview
@Composable
private fun PreviewAssetInterestItem() {
    InterestDashboardAssetItem(
        CryptoCurrency.BTC,
        AssetInterestDetail(
            totalInterest = Money.fromMajor(CryptoCurrency.BTC, 1.toBigDecimal()),
            totalBalance = Money.fromMajor(CryptoCurrency.BTC, 123.toBigDecimal()),
            rate = 12.34,
            eligible = true,
            ineligibilityReason = IneligibilityReason.KYC_TIER,
            totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 3.toBigDecimal())
        ),
        true
    )
}