package com.blockchain.home.presentation.earn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun EarnAssets(
    assetActionsNavigation: AssetActionsNavigation,
    viewModel: EarnViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: EarnViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(EarnIntent.LoadEarnAccounts)
        onDispose { }
    }

    if (viewState == EarnViewState.None) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.common_earn),
                style = AppTheme.typography.body2,
                color = Grey700
            )

            Spacer(modifier = Modifier.weight(1f))

            if (viewState is EarnViewState.Assets) {
                Text(
                    modifier = Modifier.clickableNoEffect { assetActionsNavigation.earnRewards() },
                    text = stringResource(R.string.manage),
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.primary,
                )
            }
        }
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 3.dp
        ) {
            when (viewState) {
                EarnViewState.NoAssetsInvested -> NoAssetsInvested { assetActionsNavigation.earnRewards() }
                is EarnViewState.Assets -> EarnedAssets(viewState as EarnViewState.Assets)
                EarnViewState.None -> throw IllegalStateException("No render for state None")
            }
        }
    }
}

@Composable
private fun EarnedAssets(viewState: EarnViewState.Assets) {
    Column {
        viewState.assets.forEachIndexed { index, asset ->
            BalanceTableRow(
                titleStart = buildAnnotatedString {
                    append(
                        if (asset.type == EarnType.STAKING)
                            stringResource(
                                id = R.string.staking_asset, asset.currency.name
                            ) else asset.currency.name
                    )
                },
                startImageResource = ImageResource.Remote(asset.currency.logo),
                titleEnd = buildAnnotatedString { append(asset.balance.toStringWithSymbol()) },
                bodyEnd = buildAnnotatedString {
                    append(
                        viewState.rateForAsset(
                            asset
                        )?.let {
                            stringResource(id = R.string.earn_rate_apy, it.withoutTrailingZerosIfWhole())
                        }.orEmpty()
                    )
                }
            )
            if (index < viewState.assets.size - 1) {
                Divider(color = Color(0XFFF1F2F7))
            }
        }
    }
}

private fun Double.withoutTrailingZerosIfWhole(): Any {
    return if (this.compareTo(this.toLong()) == 0)
        String.format("%d", this.toLong()) else String.format("%s", this)
}

@Preview
@Composable
private fun NoAssetsInvested(earn: () -> Unit = {}) {
    TableRow(
        contentStart = {
            Box {
                Canvas(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.large_spacing))
                        .align(Center),
                    onDraw = {
                        drawCircle(
                            color = Grey400,
                        )
                    }
                )
                Text(
                    modifier = Modifier.align(Center),
                    text = "%",
                    style = AppTheme.typography.body2,
                    color = Color.White,
                )
            }
        },
        content = {
            Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                Text(
                    text = stringResource(id = R.string.earn_up_to),
                    style = AppTheme.typography.caption2,
                    color = Grey900
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = stringResource(id = R.string.put_your_crypto_to_work),
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
                            id = R.string.common_earn
                        ).uppercase(),
                        color = Color.White,
                        style = AppTheme.typography.paragraphMono
                    )
                },
                onClick = {
                    earn()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Grey800)
            )
        }
    )
}
