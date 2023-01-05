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
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivitySectionCard
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.activity.list.composable.DUMMY_DATA
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeActivity(
    openAllActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
) {
    val walletMode by get<WalletModeService>(scope = payloadScope).walletMode
        .collectAsStateLifecycleAware(null)

    walletMode?.let {
        when (walletMode) {
            WalletMode.CUSTODIAL_ONLY -> CustodialHomeActivity(
                openAllActivity = openAllActivity,
                activityOnClick = {
                    openActivityDetail(it, WalletMode.CUSTODIAL_ONLY)
                }
            )
            WalletMode.NON_CUSTODIAL_ONLY -> PrivateKeyHomeActivity(
                openAllActivity = openAllActivity,
                activityOnClick = {
                    openActivityDetail(it, WalletMode.NON_CUSTODIAL_ONLY)
                }
            )
            else -> error("unsupported")
        }
    }
}

@Composable
fun CustodialHomeActivity(
    viewModel: CustodialActivityViewModel = getViewModel(scope = payloadScope),
    openAllActivity: () -> Unit,
    activityOnClick: (String) -> Unit
) {
    val viewState: ActivityViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityIntent.LoadActivity(SectionSize.Limited()))
        onDispose { }
    }

    HomeActivityScreen(
        activity = viewState.activity.map { it[TransactionGroup.Combined] ?: listOf() },
        onSeeAllCryptoAssetsClick = openAllActivity,
        activityOnClick = activityOnClick
    )
}

@Composable
fun PrivateKeyHomeActivity(
    viewModel: PrivateKeyActivityViewModel = getViewModel(scope = payloadScope),
    openAllActivity: () -> Unit,
    activityOnClick: (String) -> Unit
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
            activityOnClick = activityOnClick
        )
    }
}

@Composable
fun HomeActivityScreen(
    activity: DataResource<List<ActivityComponent>>,
    onSeeAllCryptoAssetsClick: () -> Unit,
    activityOnClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        if ((activity as? DataResource.Data)?.data?.isNotEmpty() == true) {
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
        }

        when (activity) {
            DataResource.Loading -> {
                ShimmerLoadingCard()
            }
            is DataResource.Error -> {
                // todo
            }
            is DataResource.Data -> {
                if (activity.data.isNotEmpty()) {
                    ActivitySectionCard(
                        components = activity.data,
                        onClick = {
                            (it as? ClickAction.Stack)?.data?.let { data ->
                                activityOnClick(data)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeActivityScreen() {
    HomeActivityScreen(
        activity = DUMMY_DATA.map { it[it.keys.first()]!! },
        onSeeAllCryptoAssetsClick = {},
        activityOnClick = {}
    )
}
