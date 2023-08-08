package com.dex.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.option.ChipOption
import com.blockchain.componentlib.option.ChipOptionsGroup
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun SettingsBottomSheet(
    closeClicked: () -> Unit,
    viewModel: SettingsViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(SettingsIntent.LoadAvailableSlippages)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        analytics.logEvent(DexAnalyticsEvents.SettingsOpened)
    }

    val viewState: SettingsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    SettingsBottomSheetScreen(
        slippages = viewState.slippages,
        updateSlippage = {
            viewModel.onIntent(
                SettingsIntent.UpdateSelectedSlippage(
                    it.factor
                )
            )
            analytics.logEvent(DexAnalyticsEvents.SlippageChanged)
            closeClicked()
        },
        closeClicked = closeClicked,
    )
}

@Composable
private fun SettingsBottomSheetScreen(
    slippages: List<Slippage>,
    updateSlippage: (Slippage) -> Unit,
    closeClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(
            title = stringResource(id = R.string.allowed_slippage),
            onClosePress = closeClicked,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.background)
        ) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            ChipOptionsGroup(
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
                options = slippages.map {
                    ChipOption(
                        id = it.factor,
                        text = "${(it.factor * 100)} %",
                        isActive = it.selected
                    )
                },
                onClick = { option ->
                    updateSlippage(slippages.first { it.factor == option.id })
                }
            )
            Text(
                modifier = Modifier.padding(all = AppTheme.dimensions.smallSpacing),
                text = stringResource(id = R.string.slippage_explanation),
                style = AppTheme.typography.paragraph1,
                color = AppColors.body
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSettingsBottomSheetScreen() {
    SettingsBottomSheetScreen(
        slippages = listOf(
            Slippage(0.5, false),
            Slippage(1.0, false),
            Slippage(1.5, true),
            Slippage(2.0, false),
        ),
        updateSlippage = {},
        closeClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSettingsBottomSheetScreenDark() {
    PreviewSettingsBottomSheetScreen()
}
