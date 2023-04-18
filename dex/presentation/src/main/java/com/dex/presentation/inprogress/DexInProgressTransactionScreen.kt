package com.dex.presentation.inprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme

@Preview
@Composable
fun DexInProgressTransactionScreen(
    onBackPressed: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                all = AppTheme.dimensions.smallSpacing
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Your transaction has been Completed")
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Well Done",
            onClick = {}
        )
    }
}