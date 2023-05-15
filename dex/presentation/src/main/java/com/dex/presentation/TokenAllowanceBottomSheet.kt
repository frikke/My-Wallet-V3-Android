package com.dex.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.dex.presentation.enteramount.AllowanceTxUiData
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.getViewModel

@Composable
fun TokenAllowanceBottomSheet(
    viewModel: TokenAllowanceViewModel = getViewModel(scope = payloadScope),
    savedStateHandle: SavedStateHandle?,
    allowanceTxUiData: AllowanceTxUiData,
    closeClicked: () -> Unit = {}
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(AllowanceIntent.FetchAllowanceTxDetails(allowanceTxUiData))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val viewState: AllowanceViewState by viewModel.viewState.collectAsStateLifecycleAware()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = closeClicked,
            startImageResource = ImageResource.None,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        viewState.assetInfo?.let {
            AssetAllowanceDescription(it)
        }

        safeLet(viewState.nativeAsset, viewState.networkFee) { asset, fee ->
            AllowanceFee(asset, fee)
            Divider()
            FeeNetwork(asset)
        }

        ApproveAndDenyButtons(
            onApprove = {
                savedStateHandle?.set(ALLOWANCE_TRANSACTION_APPROVED, true)
                closeClicked()
            },
            onDecline = {
                savedStateHandle?.set(ALLOWANCE_TRANSACTION_APPROVED, false)
                closeClicked()
            }
        )
        Spacer(modifier = Modifier.size(navBarHeight))
    }
}

@Composable
private fun AssetAllowanceDescription(asset: AssetInfo) {
    Image(imageResource = ImageResource.Remote(asset.logo, size = 88.dp))
    Text(
        modifier = Modifier.padding(
            start = AppTheme.dimensions.standardSpacing,
            end = AppTheme.dimensions.standardSpacing,
            top = AppTheme.dimensions.standardSpacing
        ),
        text = stringResource(
            id = com.blockchain.stringResources.R.string.allow_bcdc_dex_to_use,
            asset.displayTicker
        ),
        textAlign = TextAlign.Center,
        style = AppTheme.typography.title3,
        color = AppTheme.colors.title
    )

    Text(
        modifier = Modifier.padding(
            horizontal = AppTheme.dimensions.standardSpacing,
            vertical = AppTheme.dimensions.tinySpacing
        ),
        textAlign = TextAlign.Center,
        text = stringResource(
            id = com.blockchain.stringResources.R.string.allowance_bcdc_expanation,
            asset.displayTicker
        ),
        style = AppTheme.typography.body1,
        color = AppTheme.colors.body
    )
}

@Composable
fun ApproveAndDenyButtons(onApprove: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.smallestSpacing
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.tinySpacing)
    ) {
        MinimalButton(
            text = stringResource(id = com.blockchain.stringResources.R.string.common_decline),
            onClick = onDecline,
            modifier = Modifier.weight(.5f)
        )
        PrimaryButton(
            text = stringResource(id = com.blockchain.stringResources.R.string.common_approve),
            onClick = onApprove,
            modifier = Modifier.weight(.5f)
        )
    }
}

@Composable
private fun AllowanceFee(assetInfo: AssetInfo, estimatedFiatFee: String) {
    Row(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.standardSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    imageResource = ImageResource.Remote(
                        url = assetInfo.logo,
                        size = AppTheme.dimensions.smallSpacing
                    )
                )
                Text(
                    modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallestSpacing),
                    text = "~$estimatedFiatFee",
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            }
            Text(
                text = stringResource(id = com.blockchain.stringResources.R.string.estimated_fees),
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.muted
            )
        }
    }
}

@Composable
private fun FeeNetwork(assetInfo: AssetInfo) {
    Column(
        Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.standardSpacing
            )
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = AppTheme.dimensions.composeSmallestSpacing)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_wallet),
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.body
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_network),
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.body
            )
        }

        Row(
            modifier = Modifier
                .padding(top = AppTheme.dimensions.composeSmallestSpacing)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = com.blockchain.stringResources.R.string.defi_wallet_name),
                style = AppTheme.typography.body1,
                color = AppTheme.colors.title
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    imageResource = ImageResource.Remote(
                        assetInfo.logo,
                        size = AppTheme.dimensions.smallSpacing
                    )
                )
                Text(
                    modifier = Modifier
                        .padding(start = AppTheme.dimensions.smallestSpacing),
                    text = assetInfo.name,
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.title
                )
            }
        }
    }
}

const val ALLOWANCE_TRANSACTION_APPROVED = "ALLOWANCE_TRANSACTION_APPROVED"
