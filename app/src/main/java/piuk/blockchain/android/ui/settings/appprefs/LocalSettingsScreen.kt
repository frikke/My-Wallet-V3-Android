package piuk.blockchain.android.ui.settings.appprefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel
import piuk.blockchain.android.R

@Composable
fun LocalSettings(
    viewModel: LocalSettingsViewModel = getViewModel(scope = payloadScope)
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: LocalSettingsViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(LocalSettingsIntent.LoadLocalSettings)
        onDispose { }
    }

    viewState?.let { state ->
        LocalSettingsScreen(state = state, viewModel = viewModel)
    }
}

@Composable
fun LocalSettingsScreen(state: LocalSettingsViewState, viewModel: LocalSettingsViewModel) {
    AppTheme {
        AppSurface {
            Column(modifier = Modifier.fillMaxSize()) {
                ToggleTableRow(
                    primaryText = stringResource(R.string.settings_chart_vibration),
                    secondaryText = stringResource(R.string.settings_chart_vibration_desc),
                    onCheckedChange = { isChecked ->
                        viewModel.onIntent(LocalSettingsIntent.ToggleChartVibration(isVibrationEnabled = isChecked))
                    },
                    isChecked = if (state is LocalSettingsViewState.Data) {
                        state.isChartVibrationEnabled
                    } else {
                        false
                    }
                )

                HorizontalDivider(dividerColor = AppTheme.colors.medium)

                ToggleTableRow(
                    primaryText = stringResource(R.string.settings_dust_title),
                    secondaryText = stringResource(R.string.settings_dust_desc),
                    onCheckedChange = { isChecked ->
                        viewModel.onIntent(LocalSettingsIntent.ToggleSmallBalances(areSmallBalancesEnabled = isChecked))
                    },
                    isChecked = if (state is LocalSettingsViewState.Data) {
                        state.areSmallBalancesEnabled
                    } else {
                        false
                    }
                )

                HorizontalDivider(dividerColor = AppTheme.colors.medium)
            }
        }
    }
}
