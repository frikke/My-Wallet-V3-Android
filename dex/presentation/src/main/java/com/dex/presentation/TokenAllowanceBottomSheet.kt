package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Gas
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
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
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = closeClicked
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        viewState.assetInfo?.let {
            AssetAllowanceDescription(it)
        }
        LazyColumn(
            modifier = Modifier
                .padding(all = AppTheme.dimensions.smallSpacing)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            safeLet(
                viewState.networkName, viewState.networkFeeCrypto, viewState.networkFeeFiat
            ) { networkName, feeCrypto, feeFiat ->
                item(
                    content = {
                        AllowanceFee(
                            networkName = networkName,
                            estimatedFiatFee = feeFiat,
                            estimatedCryptoFee = feeCrypto
                        )
                    },
                )
            }

            safeLet(
                viewState.accountLabel,
                viewState.nativeAssetBalanceCrypto,
                viewState.nativeAssetBalanceFiat,
                viewState.nativeAsset?.logo
            ) { label, balanceCrypto, balanceFiat, logo ->
                item(
                    content = {
                        NativeAssetBalance(
                            accountLabel = label,
                            balanceCrypto = balanceCrypto,
                            balanceFiat = balanceFiat,
                            logo = logo,
                            address = viewState.receiveAddress
                        )
                    }
                )
            }
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
        MinimalPrimaryButton(
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

@Preview
@Composable
private fun PreviewApproveAndDenyButtons() {
    ApproveAndDenyButtons({}, {})
}

@Composable
private fun AllowanceFee(
    networkName: String,
    estimatedFiatFee: String,
    estimatedCryptoFee: String
) {
    BalanceTableRow(
        startImageResource = Icons.Gas.withTint(AppColors.title),
        titleStart = buildAnnotatedString {
            append(stringResource(id = com.blockchain.stringResources.R.string.estimated_fees))
        },
        backgroundShape = RoundedCornerShape(
            topStart = AppTheme.dimensions.mediumSpacing,
            topEnd = AppTheme.dimensions.mediumSpacing
        ),
        titleEnd = buildAnnotatedString {
            append("~ $estimatedFiatFee")
        },
        bodyStart = buildAnnotatedString { append(networkName) },
        bodyEnd = buildAnnotatedString {
            append(estimatedCryptoFee)
        }
    )
}

@Composable
private fun NativeAssetBalance(
    accountLabel: String,
    balanceFiat: String,
    balanceCrypto: String,
    logo: String,
    address: String?,
) {
    BalanceTableRow(
        startImageResource = ImageResource.Remote(logo),
        titleStart = buildAnnotatedString {
            append(accountLabel)
        },
        backgroundShape = RoundedCornerShape(
            bottomStart = AppTheme.dimensions.mediumSpacing,
            bottomEnd = AppTheme.dimensions.mediumSpacing
        ),
        titleEnd = buildAnnotatedString {
            append(balanceFiat)
        },
        bodyStart = buildAnnotatedString { append(address.orEmpty()) },
        bodyEnd = buildAnnotatedString {
            append(balanceCrypto)
        }
    )
}

const val ALLOWANCE_TRANSACTION_APPROVED = "ALLOWANCE_TRANSACTION_APPROVED"
