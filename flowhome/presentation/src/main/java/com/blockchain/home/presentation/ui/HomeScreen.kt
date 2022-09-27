package com.blockchain.home.presentation.ui

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = getViewModel(scope = payloadScope)) {
    Button(onClick = {
        viewModel.onIntent(HomeIntent.LoadHomeAccounts)
    }) {
        Text(text = "Accounts please")
    }
}
