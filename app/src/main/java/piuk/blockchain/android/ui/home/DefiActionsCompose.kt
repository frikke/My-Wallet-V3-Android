package piuk.blockchain.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun DefiBuyCrypto(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() }
            .background(AppTheme.colors.background)
            .padding(
                horizontal = dimensionResource(id = R.dimen.standard_margin),
                vertical = dimensionResource(id = R.dimen.standard_margin)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.light,
                    shape = RoundedCornerShape(
                        12.dp
                    )
                )
                .padding(
                    horizontal = dimensionResource(id = R.dimen.small_margin),
                    vertical = dimensionResource(id = R.dimen.small_margin)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(imageResource = ImageResource.Local(R.drawable.ic_defi_buy))
            Column(
                modifier = Modifier
                    .weight(1f, true)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_margin)
                    )
                    .align(Alignment.Top)
            ) {
                Text(
                    text = "Buy Crypto",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
                Spacer(Modifier.height(dimensionResource(id = R.dimen.smallest_margin)))
                Text(
                    text = "Top up in your Trading Account.",
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }
        }
    }
}

@Composable
fun ActionRows(data: List<DefiAction>) {
    LazyColumn {
        items(
            items = data,
        ) {
            ActionRow(
                item = it
            )
        }
    }
}

@Composable
private fun ActionRow(item: DefiAction) {
    DefaultTableRow(
        primaryText = item.title,
        secondaryText = item.subtitle,
        startImageResource = ImageResource.Local(item.icon),
        endImageResource = ImageResource.Local(R.drawable.ic_chevron_end),
        onClick = item.onClick
    )
    Divider(color = AppTheme.colors.light, thickness = 1.dp)
}

@Preview
@Composable
private fun PreviewDefiBuyCrypto() {
    AppTheme {
        DefiBuyCrypto(onClick = {})
    }
}
