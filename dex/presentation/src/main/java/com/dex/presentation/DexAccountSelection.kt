package com.dex.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.tablerow.NonCustodialAssetBalanceTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.dex.presentation.R
import com.dex.domain.DexAccount
import info.blockchain.balance.isLayer2Token

@Composable
fun DexAccountSelection(
    accounts: List<DexAccount>,
    onAccountSelected: (DexAccount) -> Unit,
    onSearchTermUpdated: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CancelableOutlinedSearch(
            onValueChange = {
                onSearchTermUpdated(it)
            },
            placeholder = stringResource(R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
        ) {
            if (accounts.any { it.balance.isPositive }) {
                if (!accounts.all { it.balance.isPositive }) {
                    paddedItem(
                        paddingValues = PaddingValues(horizontal = 16.dp)
                    ) {
                        TableRowHeader(
                            title = stringResource(com.blockchain.componentlib.R.string.your_assets),
                        )
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                    }
                }
                roundedCornersItems(
                    items = accounts.filter { it.balance.isPositive },
                    key = {
                        it.currency.networkTicker
                    },
                    content = { dexAccount ->
                        NonCustodialAssetBalanceTableRow(
                            title = dexAccount.currency.name,
                            subtitle = dexAccount.currency.takeIf { it.isLayer2Token }?.coinNetwork?.shortName ?: "",
                            valueCrypto = dexAccount.balance.toStringWithSymbol(),
                            valueFiat = dexAccount.fiatBalance.toStringWithSymbol(),
                            icon = StackedIcon.SingleIcon(
                                icon = ImageResource.Remote(dexAccount.currency.logo)
                            ),
                            onClick = {
                                onAccountSelected(dexAccount)
                            }
                        )
                        if (accounts.last() != dexAccount) {
                            Divider(color = Color(0XFFF1F2F7))
                        }
                    }
                )

                item {
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_spacing)))
                }
            }
            if (accounts.any { it.balance.isZero }) {
                if (!accounts.all { it.balance.isZero }) {

                    paddedItem(
                        paddingValues = PaddingValues(horizontal = 16.dp)
                    ) {
                        TableRowHeader(
                            title = stringResource(com.blockchain.componentlib.R.string.all_tokens),
                        )
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                    }
                }

                roundedCornersItems(
                    items = accounts.filterNot { it.balance.isPositive },
                    key = {
                        it.currency.networkTicker
                    },
                    content = { dexAccount ->
                        NoBalanceDexAccountTableRow(dexAccount, onAccountSelected)
                        if (accounts.last() != dexAccount) {
                            Divider(color = Color(0XFFF1F2F7))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NoBalanceDexAccountTableRow(dexAccount: DexAccount, onAccountSelected: (DexAccount) -> Unit) {
    TableRow(
        contentStart = {
            Image(
                modifier = Modifier
                    .size(AppTheme.dimensions.standardSpacing),
                imageResource = ImageResource.Remote(
                    url = dexAccount.currency.logo,
                ),
            )
        },
        content = {
            SmallHorizontalSpacer()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    SimpleText(
                        text = dexAccount.currency.name,
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    if (dexAccount.currency.isLayer2Token) {
                        SmallestVerticalSpacer()
                        DefaultTag(text = dexAccount.currency.coinNetwork!!.shortName)
                    }
                }

                Spacer(modifier = Modifier.weight(1F))
                Image(Icons.ChevronRight)
            }
        },
        onContentClicked = {
            onAccountSelected(dexAccount)
        }
    )
}
