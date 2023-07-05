package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.stringResources.R
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.homeFailedBalances(
    failedNetworkNames: ImmutableList<String>?,
    dismissFailedNetworksWarning: () -> Unit,
    learnMoreOnClick: () -> Unit
) {
    failedNetworkNames?.let {
        paddedItem(
            paddingValues = {
                PaddingValues(AppTheme.dimensions.smallSpacing)
            }
        ) {
            FailedBalances(
                networkNames = it,
                learnMoreOnClick = learnMoreOnClick,
                closeOnClick = dismissFailedNetworksWarning
            )
        }
    }
}

@Composable
private fun FailedBalances(
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
