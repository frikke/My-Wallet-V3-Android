package com.blockchain.blockchaincard.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.blockchaincard.R

@Composable
fun BlockchainCardScreen() {
    OrderCardScreen()
}

@Composable
private fun OrderCardScreen() {
    Column {
        Image(painter = painterResource(id = R.drawable.ic_graphic_cards),contentDescription = "Blockchain Card")
        Text(text = "Your Gateway To The Blockchain Debit Card")
        Text(text = "A card that lets you spend and earn in crypto right from your Blockchain account.")
        Spacer(Modifier.size(115.dp))
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Order My Card", color = Color.White, modifier = Modifier.background(color = Color.Blue, shape = RoundedCornerShape(3.dp)))
        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Order My Card", color = Color.Blue,)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOrderCardScreen() {
    OrderCardScreen()
}