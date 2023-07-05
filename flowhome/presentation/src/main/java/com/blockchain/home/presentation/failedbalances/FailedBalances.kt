package com.blockchain.home.presentation.failedbalances

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.QuestionOff
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.MaskableBalanceChangeTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.data.map
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.presentation.allassets.CustodialAssetState
import com.blockchain.home.presentation.allassets.FiatAssetState
import com.blockchain.home.presentation.allassets.HomeAsset
import com.blockchain.home.presentation.allassets.HomeCryptoAsset
import com.blockchain.home.presentation.allassets.NonCustodialAssetState
import com.blockchain.home.presentation.allassets.composable.BalanceWithFiatAndCryptoBalance
import com.blockchain.home.presentation.allassets.composable.BalanceWithPriceChange
import com.blockchain.stringResources.R
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.homeFailedBalances(
    failedNetworkNames: ImmutableList<String>?,
    dismissFailedNetworksWarning: () -> Unit,
    failedNetworksLearnMore: () -> Unit
) {
    failedNetworkNames?.let {
        paddedItem(
            paddingValues = {
                PaddingValues(AppTheme.dimensions.smallSpacing)
            }
        ) {
            FailedNetworksPartial(
                networkNames = it,
                learnMoreOnClick = failedNetworksLearnMore,
                closeOnClick = dismissFailedNetworksWarning
            )
        }
    }
}

@Composable
private fun FailedNetworksPartial(
    networkNames: ImmutableList<String>,
    learnMoreOnClick: () -> Unit,
    closeOnClick: () -> Unit
) {
    require(networkNames.isNotEmpty())

    CardAlert(
        title = stringResource(R.string.balances_failed_title),
        subtitle = if (networkNames.size == 1) {
            stringResource(R.string.balances_failed_description_one, networkNames.first())
        } else {
            stringResource(
                R.string.balances_failed_description_many,
                networkNames.dropLast(1).joinToString(","),
                networkNames.last()
            )
        },
        alertType = AlertType.Warning,
        onClose = closeOnClick,
        primaryCta = CardButton(
            text = stringResource(R.string.common_learn_more),
            onClick = learnMoreOnClick
        )
    )
}
