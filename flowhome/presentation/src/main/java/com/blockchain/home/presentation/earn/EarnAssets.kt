package com.blockchain.home.presentation.earn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import org.koin.androidx.compose.get

internal fun LazyListScope.homeEarnAssets(
    earnState: EarnViewState,
    earnViewModel: EarnViewModel,
    openEarnDashboard: () -> Unit
) {
    if (earnState == EarnViewState.None) {
        return
    }
    paddedItem(
        paddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        val analytics: Analytics = get()
        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
        TableRowHeader(
            title = stringResource(com.blockchain.stringResources.R.string.common_earn),
            actionTitle = stringResource(com.blockchain.stringResources.R.string.manage).takeIf {
                earnState is EarnViewState.Assets
            },
            actionOnClick = {
                openEarnDashboard()
                analytics.logEvent(DashboardAnalyticsEvents.EarnManageClicked)
            }.takeIf { earnState is EarnViewState.Assets }
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    }
    when (earnState) {
        EarnViewState.NoAssetsInvested ->
            paddedItem(
                paddingValues = PaddingValues(horizontal = 16.dp)
            ) {
                NoAssetsInvested(openEarnDashboard = openEarnDashboard)
            }
        is EarnViewState.Assets -> {
            val mAssets = earnState.assets
            paddedRoundedCornersItems(
                items = mAssets.toList(),
                key = { it.type.hashCode() + it.currency.networkTicker.hashCode() },
                paddingValues = PaddingValues(horizontal = 16.dp)
            ) { asset ->
                BalanceTableRow(
                    titleStart = buildAnnotatedString {
                        append(asset.currency.name)
                    },
                    onClick = {
                        earnViewModel.onIntent(
                            EarnIntent.AssetSelected(
                                asset
                            )
                        )
                    },
                    startImageResource = ImageResource.Remote(asset.currency.logo),
                    tags = listOf(
                        TagViewState(
                            when (asset.type) {
                                EarnType.INTEREST -> stringResource(
                                    id = com.blockchain.stringResources.R.string.earn_rewards_label_passive
                                )
                                EarnType.STAKING -> stringResource(
                                    id = com.blockchain.stringResources.R.string.earn_rewards_label_staking
                                )
                                EarnType.ACTIVE -> stringResource(
                                    id = com.blockchain.stringResources.R.string.earn_rewards_label_active
                                )
                            },
                            TagType.Default()
                        )
                    ),
                    isInlineTags = true,
                    titleEnd = buildAnnotatedString { append(asset.balance.toStringWithSymbol()) },
                    bodyEnd = buildAnnotatedString {
                        append(
                            (earnState as? EarnViewState.Assets)?.rateForAsset(
                                asset
                            )?.let {
                                "${it.withOnlyOneTrailingZeroIfWhole()}%"
                            }.orEmpty()
                        )
                    }
                )
            }
        }
        EarnViewState.None -> {
        }
    }
}

private fun Double.withOnlyOneTrailingZeroIfWhole(): String {
    return if (this.compareTo(this.toLong()) == 0) {
        String.format("%.1f", this)
    } else String.format("%s", this)
}

@Preview
@Composable
fun NoAssetsInvested(
    analytics: Analytics = get(),
    openEarnDashboard: () -> Unit = {}
) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        TableRow(
            contentStart = {
                Box {
                    Canvas(
                        modifier = Modifier
                            .size(dimensionResource(id = com.blockchain.componentlib.R.dimen.large_spacing))
                            .align(Center),
                        onDraw = {
                            drawCircle(
                                color = Grey400
                            )
                        }
                    )
                    Text(
                        modifier = Modifier.align(Center),
                        text = "%",
                        style = AppTheme.typography.body2,
                        color = Color.White
                    )
                }
            },
            content = {
                Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                    Text(
                        text = stringResource(id = com.blockchain.stringResources.R.string.earn_up_to),
                        style = AppTheme.typography.caption2,
                        color = Grey900
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = stringResource(id = com.blockchain.stringResources.R.string.put_your_crypto_to_work),
                        style = AppTheme.typography.paragraph1,
                        color = Grey900
                    )
                }
            },
            contentEnd = {
                Button(
                    modifier = Modifier
                        .wrapContentWidth(align = End)
                        .weight(1f),
                    content = {
                        Text(
                            text = stringResource(
                                id = com.blockchain.stringResources.R.string.common_earn
                            ).uppercase(),
                            color = Color.White,
                            style = AppTheme.typography.paragraphMono
                        )
                    },
                    onClick = {
                        openEarnDashboard()
                        analytics.logEvent(DashboardAnalyticsEvents.EarnGetStartedClicked)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Grey800)
                )
            }
        )
    }
}
