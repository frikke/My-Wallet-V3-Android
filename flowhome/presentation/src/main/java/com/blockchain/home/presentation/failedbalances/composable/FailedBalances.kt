package com.blockchain.home.presentation.failedbalances.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStoreOwner
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.ScreenStatusIcon
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.failedbalances.FailedBalancesViewModel
import com.blockchain.home.presentation.failedbalances.FailedBalancesViewState
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.getViewModel

@Composable
fun FailedBalances(
    dismiss: () -> Unit
) {
    val viewModel: FailedBalancesViewModel = getViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        scope = payloadScope
    )
    val viewState: FailedBalancesViewState by viewModel.viewState.collectAsStateLifecycleAware()

    (viewState.failedNetworkNames as? DataResource.Data)?.data?.let {
        FailedBalancesScreen(
            networkNames = it.toImmutableList(),
            dismiss = dismiss
        )
    }
}

@Composable
private fun FailedBalancesScreen(
    networkNames: ImmutableList<String>,
    dismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = dismiss,
        )

        Column(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenStatusIcon(
                main = Icons.Network,
                tag = Icons.AlertOn.withTint(AppColors.warning)
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Text(
                text = stringResource(R.string.balances_failed_title),
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            Text(
                text = if (networkNames.size == 1) {
                    stringResource(R.string.balances_failed_description_one, networkNames.first())
                } else {
                    stringResource(
                        R.string.balances_failed_description_many,
                        networkNames.dropLast(1).joinToString(","),
                        networkNames.last()
                    )
                },
                style = AppTheme.typography.body1,
                color = AppTheme.colors.body,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing),
            text = stringResource(R.string.common_ok),
            onClick = dismiss
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
    }
}

@Preview
@Composable
private fun PreviewFailedBalancesScreen() {
    FailedBalancesScreen(
        networkNames = persistentListOf("Ethereum"),
        dismiss = { }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFailedBalancesScreenDark() {
    PreviewFailedBalancesScreen()
}
