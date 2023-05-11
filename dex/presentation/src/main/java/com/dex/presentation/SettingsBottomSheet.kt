package com.dex.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.chip.ChipState
import com.blockchain.componentlib.option.ChipOption
import com.blockchain.componentlib.option.ChipOptionsGroup
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun SettingsBottomSheet(
    closeClicked: () -> Unit,
    viewModel: SettingsViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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

    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(
            title = stringResource(id = R.string.allowed_slippage),
            onClosePress = closeClicked,
            startImageResource = ImageResource.None,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        ChipOptionsGroup(
            options = viewState.slippages.map {
                ChipOption(
                    text = "${(it.factor * 100)} %",
                    initialState = if (it.selected) {
                        ChipState.Selected
                    } else {
                        ChipState.Enabled
                    },
                    onClick = {
                        viewModel.onIntent(
                            SettingsIntent.UpdateSelectedSlippage(
                                it.factor
                            )
                        )
                        analytics.logEvent(DexAnalyticsEvents.SlippageChanged)
                        closeClicked()
                    }
                )
            }
        )
        Text(
            modifier = Modifier.padding(all = AppTheme.dimensions.smallSpacing),
            text = stringResource(id = R.string.slippage_explanation),
            style = AppTheme.typography.paragraph1,
            color = Grey700
        )
        Spacer(modifier = Modifier.size(navBarHeight))
    }
}
