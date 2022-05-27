package com.blockchain.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.screens.DefaultPhrase
import com.blockchain.presentation.screens.DefaultPhraseScreen
import com.blockchain.presentation.screens.Splash
import com.blockchain.presentation.screens.SplashScreen
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.viewModel
import org.koin.core.scope.Scope

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
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(navigationEvent = BackPhraseDestination.Splash) {
            Splash(navController)
        }
        composable(navigationEvent = BackPhraseDestination.DefaultPhrase) {
            DefaultPhrase(getViewModel(scope = payloadScope), navController)
        }
    }
}

@Composable
inline fun <reified T : ViewModel> getViewModel(
    scope: Scope
): Lazy<T> {
    val owner = getComposeViewModelOwner()
    return scope.viewModel(owner = { owner })
}

@Composable
fun getComposeViewModelOwner(): ViewModelOwner {
    return ViewModelOwner.from(
        LocalViewModelStoreOwner.current!!,
        LocalSavedStateRegistryOwner.current
    )
}

sealed class BackPhraseDestination(override val route: String) : ComposeNavigationDestination {
    object Splash : BackPhraseDestination("Splash")
    object DefaultPhrase : BackPhraseDestination("DefaultPhrase")
}

