package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.ActivityIntent
import com.blockchain.home.presentation.activity.ActivityViewModel
import com.blockchain.home.presentation.activity.ActivityViewState
import com.blockchain.home.presentation.activity.TransactionGroup
import com.blockchain.home.presentation.activity.TransactionState
import com.blockchain.home.presentation.activity.TransactionStatus
import com.blockchain.home.presentation.activity.composable.ActivityList
import com.blockchain.home.presentation.allassets.composable.CryptoAssetsLoading
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeActivity(
    viewModel: ActivityViewModel = getViewModel(scope = payloadScope),
    openAllActivity: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: ActivityViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited()))
        onDispose { }
    }

    viewState?.let { state ->
        HomeActivityScreen(
            activity = state.activity.map { it[TransactionGroup.Combined] ?: listOf() },
            onSeeAllCryptoAssetsClick = openAllActivity,
        )
    }
}

@Composable
fun HomeActivityScreen(
    activity: DataResource<List<TransactionState>>,
    onSeeAllCryptoAssetsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.ma_home_activity_title),
                style = AppTheme.typography.body2,
                color = Grey700
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                modifier = Modifier.clickableNoEffect(onSeeAllCryptoAssetsClick),
                text = stringResource(R.string.see_all),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.primary,
            )
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        when (activity) {
            DataResource.Loading -> {
                CryptoAssetsLoading()
            }
            is DataResource.Error -> {
                // todo
            }
            is DataResource.Data -> {
                if (activity.data.isNotEmpty()) {
                    ActivityList(transactions = activity.data)
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeActivityScreen() {
    HomeActivityScreen(
        activity = DataResource.Data(
            listOf(
                TransactionState(
                    transactionTypeIcon = "transactionTypeIcon",
                    transactionCoinIcon = "transactionCoinIcon",
                    TransactionStatus.Pending(),
                    valueTopStart = "Sent Bitcoin",
                    valueTopEnd = "-10.00",
                    valueBottomStart = "85% confirmed",
                    valueBottomEnd = "-0.00893208 ETH"
                ),
                TransactionState(
                    transactionTypeIcon = "Cashed Out USD",
                    transactionCoinIcon = "transactionCoinIcon",
                    TransactionStatus.Pending(isRbfTransaction = true),
                    valueTopStart = "Sent Bitcoin",
                    valueTopEnd = "-25.00",
                    valueBottomStart = "RBF transaction",
                    valueBottomEnd = "valueBottomEnd"
                ),
                TransactionState(
                    transactionTypeIcon = "transactionTypeIcon",
                    transactionCoinIcon = "transactionCoinIcon",
                    TransactionStatus.Settled,
                    valueTopStart = "Sent Bitcoin",
                    valueTopEnd = "-10.00",
                    valueBottomStart = "June 14",
                    valueBottomEnd = "-0.00893208 ETH"
                ),
                TransactionState(
                    transactionTypeIcon = "Cashed Out USD",
                    transactionCoinIcon = "transactionCoinIcon",
                    TransactionStatus.Canceled,
                    valueTopStart = "Sent Bitcoin",
                    valueTopEnd = "-25.00",
                    valueBottomStart = "Canceled",
                    valueBottomEnd = "valueBottomEnd"
                ),
                TransactionState(
                    transactionTypeIcon = "transactionTypeIcon",
                    transactionCoinIcon = "transactionCoinIcon",
                    TransactionStatus.Canceled,
                    valueTopStart = "Sent Bitcoin",
                    valueTopEnd = "100.00",
                    valueBottomStart = "Canceled",
                    valueBottomEnd = "0.00025 BTC"
                )
            )
        ),
        onSeeAllCryptoAssetsClick = {},
    )
}
