package com.blockchain.chrome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.navigation.AppNavigation
import com.blockchain.chrome.navigation.MultiAppNavHost
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.prices.navigation.PricesNavigation
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.core.parameter.parametersOf

class MultiAppActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val appNavigation: AppNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                navController = rememberNavController(),
                assetActionsNavigation = appNavigation as AssetActionsNavigation,
                pricesNavigation = appNavigation as PricesNavigation
            )
        }
    }

    companion object {
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, MultiAppActivity::class.java)
    }
}
