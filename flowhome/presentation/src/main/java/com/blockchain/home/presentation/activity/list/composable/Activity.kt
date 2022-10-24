package com.blockchain.home.presentation.activity.list.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.detail.composable.ActivityDetail
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewModel
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.activity.list.TransactionState
import com.blockchain.home.presentation.activity.list.TransactionStatus
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun Acitivity(
    viewModel: ActivityViewModel = getViewModel(scope = payloadScope)
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: ActivityViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.All))
        onDispose { }
    }

    viewState?.let { state ->
        ActivityScreen(
            activity = state.activity
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ActivityScreen(
    activity: DataResource<Map<TransactionGroup, List<TransactionState>>>
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    BackHandler(sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ActivityDetail()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0XFFF1F2F7))
        ) {
            NavigationBar(
                title = stringResource(R.string.ma_home_activity_title),
                onBackButtonClick = { },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                when (activity) {
                    is DataResource.Loading -> {
                        ShimmerLoadingCard()
                    }
                    is DataResource.Error -> {
                        // todo
                    }
                    is DataResource.Data -> {
                        ActivityData(
                            transactions = activity.data,
                            onActivityClick = {
                                focusManager.clearFocus(true)
                                coroutineScope.launch { sheetState.show() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityData(
    transactions: Map<TransactionGroup, List<TransactionState>>,
    onActivityClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CancelableOutlinedSearch(
            onValueChange = { },
            placeholder = stringResource(R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        ActivityGroups(
            transactions = transactions,
            onActivityClick = onActivityClick
        )
    }
}

@Composable
fun ActivityGroups(
    transactions: Map<TransactionGroup, List<TransactionState>>,
    onActivityClick: () -> Unit
) {
    LazyColumn {
        itemsIndexed(
            items = transactions.keys.toList(),
            itemContent = { index, group ->
                val transactionsList = transactions[group]!!

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.name,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.muted
                    )

                    if (group is TransactionGroup.Group /*todo waiting for how to know it's pending*/) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                        Image(ImageResource.Local(R.drawable.ic_question))
                    }
                }

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                ActivityList(
                    transactions = transactionsList,
                    onActivityClick = onActivityClick
                )

                if (index < transactionsList.toList().lastIndex) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                }
            }
        )
    }
}

@Composable
fun ActivityList(
    modifier: Modifier = Modifier,
    transactions: List<TransactionState>,
    onActivityClick: () -> Unit
) {
    if (transactions.isNotEmpty()) {
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column(modifier = modifier) {
                transactions.forEachIndexed { index, transaction ->
                    TransactionSummary(
                        status = transaction.status,
                        iconUrl = transaction.transactionTypeIcon,
                        coinIconUrl = transaction.transactionCoinIcon,
                        valueTopStart = transaction.valueTopStart,
                        valueTopEnd = transaction.valueTopEnd,
                        valueBottomStart = transaction.valueBottomStart,
                        valueBottomEnd = transaction.valueBottomEnd,
                        onClick = onActivityClick
                    )

                    if (index < transactions.lastIndex) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewActivityScreen() {
    ActivityScreen(
        activity = DataResource.Data(
            mapOf(
                TransactionGroup.Group("Pending") to listOf(
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
                    )
                ),
                TransactionGroup.Group("June") to listOf(
                    TransactionState(
                        transactionTypeIcon = "transactionTypeIcon",
                        transactionCoinIcon = "transactionCoinIcon",
                        TransactionStatus.Confirmed,
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
                ),
                TransactionGroup.Group("July") to listOf(
                    TransactionState(
                        transactionTypeIcon = "transactionTypeIcon",
                        transactionCoinIcon = "transactionCoinIcon",
                        TransactionStatus.Declined,
                        valueTopStart = "Added USD",
                        valueTopEnd = "-25.00",
                        valueBottomStart = "Declined",
                        valueBottomEnd = "valueBottomEnd"
                    ),
                    TransactionState(
                        transactionTypeIcon = "transactionTypeIcon",
                        transactionCoinIcon = "transactionCoinIcon",
                        TransactionStatus.Failed,
                        valueTopStart = "Added USD",
                        valueTopEnd = "-25.00",
                        valueBottomStart = "Failed",
                        valueBottomEnd = "valueBottomEnd"
                    )
                )
            )
        )
    )
}
