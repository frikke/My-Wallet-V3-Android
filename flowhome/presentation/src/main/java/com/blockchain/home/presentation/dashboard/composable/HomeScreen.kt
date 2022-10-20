package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun HomeScreen(
    listState: LazyListState,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0XFFF1F2F7),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
    ) {
        item {
            HomeAssets(
                openAllAssets = openCryptoAssets
            )
        }

        item {
            HomeActivity(
                openAllActivity = openActivity
            )
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}
