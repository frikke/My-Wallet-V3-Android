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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivitySectionCard
import com.blockchain.home.presentation.activity.custodial.list.CustodialActivityIntent
import com.blockchain.home.presentation.activity.custodial.list.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.detail.composable.ActivityDetail
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewModel
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.koin.payloadScope
import com.blockchain.koin.superAppModeService
import com.blockchain.utils.getMonthName
import com.blockchain.utils.toMonthAndYear
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import java.util.Calendar

@Composable
fun Activity() {
    val walletMode by get<WalletModeService>(superAppModeService).walletMode.collectAsStateLifecycleAware(null)
    walletMode?.let {
        when (walletMode) {
            WalletMode.CUSTODIAL_ONLY -> CustodialActivity()
            WalletMode.NON_CUSTODIAL_ONLY -> NonCustodialActivity()
            else -> error("unsupported")
        }
    }
}

@Composable
fun CustodialActivity(
    viewModel: CustodialActivityViewModel = getViewModel(scope = payloadScope),
) {
    val viewState: ActivityViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(CustodialActivityIntent.LoadActivity(SectionSize.All))
        onDispose { }
    }

    ActivityScreen(
        activity = viewState.activity,
        onSearchTermEntered = { term ->
            viewModel.onIntent(CustodialActivityIntent.FilterSearch(term = term))
        },
    )
}

@Composable
fun NonCustodialActivity(
    viewModel: ActivityViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: ActivityViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.All))
        onDispose { }
    }

    ActivityScreen(
        activity = viewState.activity,
        onSearchTermEntered = { term ->
            viewModel.onIntent(ActivityIntent.FilterSearch(term = term))
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ActivityScreen(
    activity: DataResource<Map<TransactionGroup, List<ActivityComponent>>>,
    onSearchTermEntered: (String) -> Unit
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
            ActivityDetail(
                onCloseClick = { coroutineScope.launch { sheetState.hide() } }
            )
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
                            activity = activity.data,
                            onSearchTermEntered = onSearchTermEntered,
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
    activity: Map<TransactionGroup, List<ActivityComponent>>,
    onSearchTermEntered: (String) -> Unit,
    onActivityClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CancelableOutlinedSearch(
            onValueChange = onSearchTermEntered,
            placeholder = stringResource(R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        ActivityGroups(
            activity = activity,
            onActivityClick = onActivityClick
        )
    }
}

@Composable
fun ActivityGroups(
    activity: Map<TransactionGroup, List<ActivityComponent>>,
    onActivityClick: () -> Unit
) {
    LazyColumn {
        itemsIndexed(
            items = activity.keys.toList(),
            itemContent = { index, group ->
                activity[group]?.let { transactionsList ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val name = when (group) {
                            TransactionGroup.Group.Pending -> "Pending" // todo str res
                            is TransactionGroup.Group.Date -> group.date.format()
                            TransactionGroup.Combined -> error("not allowed")
                        }
                        Text(
                            text = name,
                            style = AppTheme.typography.body2,
                            color = AppTheme.colors.muted
                        )

                        if (group is TransactionGroup.Group.Pending) {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                            Image(ImageResource.Local(R.drawable.ic_question))
                        }
                    }

                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                    ActivitySectionCard(
                        components = transactionsList,
                        onClick = onActivityClick
                    )

                    if (index < activity.keys.toList().lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
                    }
                }
            }
        )
    }
}

private fun Calendar.format(): String {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return if (get(Calendar.YEAR) == currentYear) {
        getMonthName()
    } else {
        toMonthAndYear()
    }
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewActivityScreen() {
    ActivityScreen(
        activity = DUMMY_DATA,
        onSearchTermEntered = {}
    )
}
