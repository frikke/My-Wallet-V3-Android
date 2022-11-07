package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.system.ShimmerLoadingBox
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.WalletBalance
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.koin.androidx.compose.getViewModel

@Composable
fun Balance(
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope)
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val viewState: AssetsViewState? by viewModel.viewState.collectAsStateLifecycleAware(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(AssetsIntent.LoadAccounts(SectionSize.Limited()))
        onDispose { }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(AssetsIntent.LoadFilters)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    viewState?.let { state ->
        BalanceScreen(walletBalance = state.balance)
    }
}

@Composable
fun BalanceScreen(
    walletBalance: DataResource<WalletBalance>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.dimensions.standardSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (walletBalance) {
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

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1F))
                    ShimmerLoadingBox(
                        modifier = Modifier
                            .height(AppTheme.dimensions.mediumSpacing)
                            .weight(0.5F)
                    )
                    Spacer(modifier = Modifier.weight(1F))
                }
            }

            is DataResource.Data -> {
                with(walletBalance.data) {
                    Text(
                        text = balance.toStringWithSymbol(),
                        style = AppTheme.typography.title1,
                        color = AppTheme.colors.title
                    )

                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    Text(
                        text = "${valueChange.indicator} ${balanceDifference.toStringWithSymbol()} (${valueChange.value}%)",
                        style = AppTheme.typography.paragraph2,
                        color = valueChange.color
                    )
                }
            }

            is DataResource.Error -> {
                Text(
                    text = "error ${walletBalance.error}",
                    style = AppTheme.typography.title1,
                    color = AppTheme.colors.title
                )

            }
        }
    }
}

//

@Preview
@Composable
fun PreviewBalanceScreen() {
    BalanceScreen(
        walletBalance = DataResource.Data(
            WalletBalance(
                balance = Money.fromMinor(CryptoCurrency.ETHER, 42000.toBigInteger()),
                balanceDifference = Money.fromMajor(CryptoCurrency.ETHER, 666.toBigDecimal()),
                valueChange = ValueChange.Up((7.18))
            )
        )
    )
}

@Preview
@Composable
fun PreviewBalanceScreenLoading() {
    BalanceScreen(
        walletBalance = DataResource.Loading
    )
}