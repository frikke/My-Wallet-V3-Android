package piuk.blockchain.android.ui.multiapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import piuk.blockchain.android.ui.superapp2.MultiAppDashboard

class MultiAppDashboardActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppDashboard()
        }
    }

    companion object{
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, MultiAppDashboardActivity::class.java)
    }

}
