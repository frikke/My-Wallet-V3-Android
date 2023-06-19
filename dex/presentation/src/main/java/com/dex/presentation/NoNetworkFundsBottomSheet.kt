package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Orange500
import com.blockchain.stringResources.R
import info.blockchain.balance.AssetCatalogue
import org.koin.androidx.compose.get

@Composable
fun NoNetworkFundsBottomSheet(
    savedStateHandle: SavedStateHandle?,
    assetTicker: String,
    assetCatalogue: AssetCatalogue = get(),
    closeClicked: () -> Unit = {}
) {

    val currency = assetCatalogue.assetInfoFromNetworkTicker(assetTicker) ?: return
    val coinNetwork = currency.coinNetwork ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = closeClicked,
            startImageResource = ImageResource.None,
            closeButtonBackground = AppTheme.colors.backgroundSecondary,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        SmallTagIcon(
            icon = StackedIcon.SmallTag(
                main = ImageResource.Remote(currency.logo),
                tag = Icons.AlertOn.withTint(Orange500)
            ),
            mainIconSize = 88.dp,
            tagIconSize = 44.dp,
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
        SimpleText(
            text = stringResource(id = R.string.no_assets_on_network, coinNetwork.shortName),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        SimpleText(
            text = stringResource(
                id = R.string.no_assets_on_network_description,
                coinNetwork.shortName,
                currency.displayTicker
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(id = R.string.common_deposit)
        ) {
            savedStateHandle?.set(DEPOSIT_FOR_ACCOUNT_REQUESTED, true)
            closeClicked()
        }
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
    }
}

const val DEPOSIT_FOR_ACCOUNT_REQUESTED = "DEPOSIT_FOR_ACCOUNT_REQUESTED"
