package piuk.blockchain.android.ui.brokerage.buy

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.NonCancelableOutlinedSearch
import com.blockchain.componentlib.system.EmbeddedFragment
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.customviews.EmptyStateView
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.CustomEmptyStateView
import piuk.blockchain.android.ui.dashboard.asPercentString

@Composable
fun BuyIntroScreen(
    buyViewState: BuyViewState,
    onSearchValueUpdated: (String) -> Unit,
    onListItemClicked: (BuyCryptoItem) -> Unit,
    onEmptyStateClicked: (BlockedReason) -> Unit,
    onErrorRetryClicked: () -> Unit,
    onErrorContactSupportClicked: () -> Intent,
    fragmentManager: FragmentManager
) {
    when (buyViewState) {
        BuyViewState.Loading -> {}
        is BuyViewState.ShowAssetList -> {
            val listItems = buyViewState.list
            val listState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .background(AppTheme.colors.light)
                    .padding(
                        top = AppTheme.dimensions.smallSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    )
            ) {
                var searchedText by remember { mutableStateOf("") }

                NonCancelableOutlinedSearch(
                    placeholder = stringResource(R.string.search_coins_hint),
                    onValueChange = {
                        searchedText = it
                        onSearchValueUpdated(it)
                    }
                )

                SmallVerticalSpacer()

                if (searchedText.isNotEmpty() && listItems.isEmpty()) {
                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.assets_no_result),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )
                } else {
                    Card(
                        backgroundColor = AppTheme.colors.background,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                        elevation = 0.dp
                    ) {
                        LazyColumn(
                            state = listState
                        ) {
                            items(
                                listItems,
                                key = {
                                    it.asset.networkTicker
                                }
                            ) { item ->
                                BuyItem(
                                    buyItem = item,
                                    onClick = { onListItemClicked(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
        is BuyViewState.ShowEmptyState -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CustomEmptyStateView(context).apply {
                    title = R.string.account_restricted
                    descriptionText = buyViewState.description
                    icon = buyViewState.icon
                    ctaText = buyViewState.ctaText
                    ctaAction = { onEmptyStateClicked(buyViewState.reason) }
                }
            }
        )
        is BuyViewState.ShowError -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                EmptyStateView(context).apply {
                    setDetails(
                        action = { onErrorRetryClicked() },
                        onContactSupport = { onErrorContactSupportClicked() },
                        contactSupportEnabled = true
                    )
                }
            }
        )
        BuyViewState.ShowKyc ->
            EmbeddedFragment(
                fragment = KycUpgradeNowSheet.newInstance(),
                fragmentManager = fragmentManager,
                tag = "KYC_Now"
            )
    }
}

@Composable
fun BuyItem(
    buyItem: BuyCryptoItem,
    onClick: () -> Unit,
) {

    val accessAssetName = stringResource(R.string.accessibility_asset_name)

    TableRow(
        contentStart = {
            Image(
                imageResource = ImageResource.Remote(
                    buyItem.asset.logo
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                    .clip(CircleShape)
            )
        },

        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        end = dimensionResource(
                            id = com.blockchain.walletconnect.R.dimen.tiny_spacing
                        )
                    )
            ) {
                Text(
                    text = buyItem.asset.name,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                    modifier = Modifier.semantics {
                        contentDescription = "$accessAssetName " + buyItem.asset.name
                    }
                )
                Row {
                    Text(
                        text = buyItem.price.toStringWithSymbol(),
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                horizontal = dimensionResource(id = com.blockchain.walletconnect.R.dimen.tiny_spacing)
                            ),
                        text = buyItem.percentageDelta.takeIf { !it.isNullOrNaN() }?.asPercentString() ?: "--",
                        style = AppTheme.typography.paragraph1,
                        color = buyItem.percentageDelta.takeIf { !it.isNullOrNaN() }?.let {
                            when {
                                it > 0 -> AppTheme.colors.success
                                it < 0 -> AppTheme.colors.error
                                else -> AppTheme.colors.primary
                            }
                        } ?: AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            Image(
                imageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                ),
            )
        },
        onContentClicked = onClick,
    )
    Divider(color = AppTheme.colors.light, thickness = 1.dp)
}

private fun Double?.isNullOrNaN(): Boolean {
    return this == null || this.isNaN()
}
