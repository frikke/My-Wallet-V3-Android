package piuk.blockchain.android.ui.multiapp.composable

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun TotalBalance(
    modifier: Modifier = Modifier,
    balance: String
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 12.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8F),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiMedium))
                )
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiMedium))
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                Text(
                    modifier = Modifier,
                    text = "Total Balance",
                    color = Color.White.copy(alpha = 0.8F),
                    style = AppTheme.typography.paragraph1
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    modifier = Modifier,
                    text = balance,
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
    TotalBalance(balance = "$278,031.12")
}