package com.blockchain.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.screens.DefaultPhrase
import com.blockchain.presentation.screens.ManualBackup
import com.blockchain.presentation.screens.Splash
import com.blockchain.presentation.screens.VerifyPhrase
import com.blockchain.presentation.viewmodel.DefaultPhraseViewModel
import org.koin.androidx.compose.getViewModel

class BackupPhraseActivity : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackupPhraseNavHost()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, BackupPhraseActivity::class.java)
    }
}

@Composable
fun BackupPhraseNavHost(
    startDestination: String = BackPhraseDestination.Splash.route
) {
    val navController = rememberNavController()
    val viewModel: DefaultPhraseViewModel = getViewModel(scope = payloadScope)

    viewModel.viewCreated(ModelConfigArgs.NoArgs)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(navigationEvent = BackPhraseDestination.Splash) {
            Splash(navController)
        }
        composable(navigationEvent = BackPhraseDestination.DefaultPhrase) {
            DefaultPhrase(viewModel, navController)
        }
        composable(navigationEvent = BackPhraseDestination.ManualBackup) {
            ManualBackup(viewModel, navController)
        }
        composable(navigationEvent = BackPhraseDestination.VerifyPhrase) {
            VerifyPhrase(viewModel, navController)
        }
    }
}

sealed class BackPhraseDestination(override val route: String) : ComposeNavigationDestination {
    object Splash : BackPhraseDestination("Splash")
    object DefaultPhrase : BackPhraseDestination("DefaultPhrase")
    object ManualBackup : BackPhraseDestination("ManualBackup")
    object VerifyPhrase : BackPhraseDestination("VerifyPhrase")
}
