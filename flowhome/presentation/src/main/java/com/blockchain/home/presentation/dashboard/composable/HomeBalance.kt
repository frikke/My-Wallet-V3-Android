package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.basic.MaskableTextWithToggle
import com.blockchain.componentlib.basic.MaskedTextFormat
import com.blockchain.componentlib.chrome.BALANCE_OFFSET_ANIM_DURATION
import com.blockchain.componentlib.chrome.BALANCE_OFFSET_TARGET
import com.blockchain.componentlib.system.ShimmerLoadingBox
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.presentation.balance.BalanceDifferenceConfig
import com.blockchain.presentation.balance.WalletBalance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money

@Composable
fun BalanceScreen(
    modifier: Modifier = Modifier,
    walletBalance: WalletBalance,
    balanceAlphaProvider: () -> Float,
    hideBalance: Boolean,
) {
    Column(
        modifier = modifier
            .padding(vertical = AppTheme.dimensions.tinySpacing)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TotalBalance(
            balance = walletBalance.balance,
            balanceAlphaProvider = balanceAlphaProvider,
            hide = hideBalance,
        )
        walletBalance.balanceDifference?.let { balanceDifference ->
            BalanceDifference(
                balanceDifference = balanceDifference
            )
        }
    }
}

@Composable
fun TotalBalance(
    balanceAlphaProvider: () -> Float,
    hide: Boolean,
    balance: DataResource<Money>
) {
    val balanceOffset by animateIntAsState(
        targetValue = if (hide) -BALANCE_OFFSET_TARGET else 0,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    when (balance) {
        DataResource.Loading -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1F))
                ShimmerLoadingBox(
                    modifier = Modifier
                        .height(AppTheme.dimensions.largeSpacing)
                        .weight(1F)
                )
                Spacer(modifier = Modifier.weight(1F))
            }
        }

        is DataResource.Data -> {
            MaskableTextWithToggle(
                modifier = Modifier
                    .clipToBounds()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = balanceOffset
                        )
                    }
                    .graphicsLayer {
                        this.alpha = balanceAlphaProvider()
                        val scale = (balanceAlphaProvider() * 1.6F).coerceIn(0F, 1F)
                        scaleX = scale
                        scaleY = scale
                    },
                clearText = balance.data.symbol,
                maskableText = balance.data.toStringWithoutSymbol(),
                format = MaskedTextFormat.ClearThenMasked,
                style = AppTheme.typography.title1,
                color = AppTheme.colors.title
            )
        }

        is DataResource.Error -> {
            // todo(othman) checking with Ethan
        }
    }
}

@Composable
fun BalanceDifference(
    balanceDifference: BalanceDifferenceConfig
) {
    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    Row {
        Text(
            text = balanceDifference.valueChange.indicator,
            style = AppTheme.typography.paragraph2,
            color = balanceDifference.valueChange.color
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        MaskableText(
            clearText = balanceDifference.differenceSymbol,
            maskableText = balanceDifference.differenceAmount,
            format = MaskedTextFormat.ClearThenMasked,
            style = AppTheme.typography.paragraph2,
            color = balanceDifference.valueChange.color
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        Text(
            text = "(${balanceDifference.valueChange.value}%)",
            style = AppTheme.typography.paragraph2,
            color = balanceDifference.valueChange.color
        )
    }
}

@Preview
@Composable
fun PreviewBalanceScreen() {
    AppTheme {
        BalanceScreen(
            walletBalance =
            WalletBalance(
                balance = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
                cryptoBalanceDifference24h = DataResource.Data(
                    Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())
                ),
                cryptoBalanceNow = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal()))
            ),
            balanceAlphaProvider = { 1F },
            hideBalance = false,
        )
    }
}

@Preview
@Composable
fun PreviewBalanceScreenLoading() {
    BalanceScreen(
        walletBalance = WalletBalance(
            balance = DataResource.Loading,
            cryptoBalanceDifference24h = DataResource.Loading,
            cryptoBalanceNow = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal()))
        ),
        balanceAlphaProvider = { 1F },
        hideBalance = false,
    )
}
