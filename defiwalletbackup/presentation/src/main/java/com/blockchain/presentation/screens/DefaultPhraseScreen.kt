package com.blockchain.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.presentation.R
import com.blockchain.presentation.viewmodel.DefaultPhraseViewModel

@Composable
fun DefaultPhraseScreen(
    viewModel: Lazy<DefaultPhraseViewModel>,
    navController: NavController
) {
    val vm: DefaultPhraseViewModel = viewModel.value
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NavigationBar(title = stringResource(id = R.string.secure_defi_wallets), onBackButtonClick = { })

        val lifecycleOwner = LocalLifecycleOwner.current
        val stateFlowLifecycleAware = remember(vm.viewState, lifecycleOwner) {
            vm.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
        }
        val state by stateFlowLifecycleAware.collectAsState(null)

        SimpleText(
            text = stringResource(id = R.string.your_recovery_phrase),
            style = ComposeTypographies.Title1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
    }
}