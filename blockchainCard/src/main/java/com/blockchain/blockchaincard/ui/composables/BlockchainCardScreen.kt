package com.blockchain.blockchaincard.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.blockchaincard.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppDimensions
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BlockchainCardScreen() {
    OrderCardScreen()
}

@Composable
private fun OrderCardScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_graphic_cards),
            contentDescription = "Blockchain Card",
            modifier = Modifier.padding(0.dp, AppTheme.dimensions.xxxPaddingLarge, AppTheme.dimensions.paddingMedium, 0.dp)
        )
        SimpleText(
            text = "Your Gateway To The Blockchain Debit Card",
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        SimpleText(
            text = "A card that lets you spend and earn in crypto right from your Blockchain account.",
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
        Spacer(Modifier.size(115.dp))
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Order My Card", color = Color.White, modifier = Modifier.background(color = Color.Blue, shape = RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.size(AppTheme.dimensions.paddingSmall))
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Already Have A Card? Link It Here", color = Color.Blue,)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOrderCardScreen() {
    AppTheme(darkTheme = false) {
        AppSurface {
            OrderCardScreen()
        }
    }
}