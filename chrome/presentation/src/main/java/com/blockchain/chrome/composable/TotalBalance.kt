package com.blockchain.chrome.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.basic.MaskedTextFormat
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.stringResources.R
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

@Composable
fun TotalBalance(
    modifier: Modifier = Modifier,
    balance: DataResource<Money>
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 12.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8F),
                    shape = RoundedCornerShape(dimensionResource(com.blockchain.componentlib.R.dimen.borderRadiiMedium))
                )
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(dimensionResource(com.blockchain.componentlib.R.dimen.borderRadiiMedium))
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                Text(
                    modifier = Modifier,
                    text = stringResource(R.string.common_total_balance),
                    color = Color.White.copy(alpha = 0.8F),
                    style = AppTheme.typography.paragraph1
                )

                Spacer(modifier = Modifier.size(8.dp))

                MaskableText(
                    modifier = Modifier,
                    clearText = balance.map { it.symbol }.dataOrElse(""),
                    maskableText = when (balance) {
                        DataResource.Loading -> stringResource(R.string.total_balance_loading)
                        is DataResource.Error -> stringResource(R.string.total_balance_error)
                        is DataResource.Data -> balance.data.toStringWithoutSymbol()
                    },
                    format = MaskedTextFormat.ClearThenMasked,
                    color = Color.White,
                    style = AppTheme.typography.paragraph2
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewTotalBalance() {
    TotalBalance(balance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, "123".toBigDecimal())))
}
