package piuk.blockchain.android.ui.interest.presentation.composables

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
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail

@Composable
fun InterestDashboardAssetItem(
    modifier: Modifier = Modifier,
    assetInfo: AssetInfo,
    assetInterestDetail: AssetInterestDetail?,
    isKycGold: Boolean,
    interestItemClicked: (AssetInfo, Boolean) -> Unit
) {
    Column(modifier = modifier.padding(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))) {
        AssetName(assetInfo)

        // if no details available, don't show the balance and apy views
        if (assetInterestDetail != null) {
            Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))

            InterestApy(assetInfo, assetInterestDetail)

            HorizontalDivider(
                modifier = Modifier
                    .padding(
                        top = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing),
                        bottom = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
                    )
                    .fillMaxWidth()
            )

            InterestBalance(assetInfo, assetInterestDetail)
        }

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))

        InterestCta(
            ctaText = stringResource(
                when {
                    assetInterestDetail == null ->
                        com.blockchain.stringResources.R.string.rewards_dashboard_item_action_earn

                    assetInterestDetail.totalBalance.isPositive ->
                        com.blockchain.stringResources.R.string.rewards_dashboard_item_action_view

                    else -> com.blockchain.stringResources.R.string.rewards_dashboard_item_action_earn
                }
            ),
            enabled = (isKycGold && (assetInterestDetail?.eligibility is EarnRewardsEligibility.Eligible)) ||
                assetInterestDetail?.totalBalance?.isPositive == true,
            onClick = {
                if (assetInterestDetail != null) {
                    interestItemClicked(assetInfo, assetInterestDetail.totalBalance.isPositive)
                }
            }
        )

        if (assetInterestDetail == null || (assetInterestDetail.eligibility is EarnRewardsEligibility.Ineligible)) {
            Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
            InterestExplainer(
                stringResource(
                    when (assetInterestDetail?.eligibility) {
                        EarnRewardsEligibility.Ineligible.REGION ->
                            com.blockchain.stringResources.R.string.rewards_item_issue_region

                        EarnRewardsEligibility.Ineligible.KYC_TIER ->
                            com.blockchain.stringResources.R.string.rewards_item_issue_kyc

                        else -> com.blockchain.stringResources.R.string.rewards_item_issue_other
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
                .size(dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing))
                .clip(CircleShape),
            imageResource = ImageResource.Remote(assetInfo.logo)
        )

        Text(
            modifier = Modifier.padding(start = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)),
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
            modifier = Modifier.padding(start = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)),
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            text = buildAnnotatedString {
                append(stringResource(id = com.blockchain.stringResources.R.string.rewards_dashboard_item_rate_1))

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${assetInterestDetail.rate}%")
                }

                append(
                    stringResource(
                        id = com.blockchain.stringResources.R.string.rewards_dashboard_item_rate_2,
                        assetInfo.name
                    )
                )
            }
        )
    }
}

@Composable
private fun InterestCta(ctaText: String, enabled: Boolean, onClick: () -> Unit) {
    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = ctaText,
        state = if (enabled) ButtonState.Enabled else ButtonState.Disabled,
        onClick = onClick
    )
}

@Composable
private fun InterestBalance(assetInfo: AssetInfo, assetInterestDetail: AssetInterestDetail) {
    Row {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                style = AppTheme.typography.caption1,
                text = stringResource(
                    id = com.blockchain.stringResources.R.string.rewards_dashboard_item_balance_title,
                    assetInfo.displayTicker
                ),
                color = Grey800
            )

            Text(
                style = AppTheme.typography.paragraph2,
                text = assetInterestDetail.totalBalance.toStringWithSymbol(),
                color = Grey800
            )
        }

        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                style = AppTheme.typography.caption1,
                text = stringResource(
                    id = com.blockchain.stringResources.R.string.rewards_dashboard_item_accrued_title
                ),
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
            modifier = Modifier.padding(
                start = dimensionResource(com.blockchain.componentlib.R.dimen.minuscule_spacing)
            ),
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
        assetInfo = CryptoCurrency.BTC,
        assetInterestDetail = null,
        isKycGold = true
    ) { _, _ -> }
}

@Preview
@Composable
private fun PreviewAssetInterestItemEligible() {
    InterestDashboardAssetItem(
        assetInfo = CryptoCurrency.BTC,
        assetInterestDetail = AssetInterestDetail(
            totalInterest = Money.fromMajor(CryptoCurrency.BTC, 1.toBigDecimal()),
            totalBalance = Money.fromMajor(CryptoCurrency.BTC, 123.toBigDecimal()),
            rate = 12.34,
            eligibility = EarnRewardsEligibility.Eligible,
            totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 3.toBigDecimal())
        ),
        isKycGold = true
    ) { _, _ -> }
}

@Preview
@Composable
private fun PreviewAssetInterestItemIneligible() {
    InterestDashboardAssetItem(
        assetInfo = CryptoCurrency.BTC,
        assetInterestDetail = AssetInterestDetail(
            totalInterest = Money.fromMajor(CryptoCurrency.BTC, 1.toBigDecimal()),
            totalBalance = Money.fromMajor(CryptoCurrency.BTC, 123.toBigDecimal()),
            rate = 12.34,
            eligibility = EarnRewardsEligibility.Ineligible.KYC_TIER,
            totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 3.toBigDecimal())
        ),
        isKycGold = true
    ) { _, _ -> }
}
