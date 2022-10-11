package piuk.blockchain.android.ui.multiapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.home.presentation.allassets.AllAssetsActivity
import com.blockchain.koin.payloadScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.androidx.compose.getViewModel
import piuk.blockchain.android.ui.multiapp.composable.MultiAppChrome

class MultiAppActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            val viewModel: MultiAppViewModel = getViewModel(scope = payloadScope)
            viewModel.viewCreated(ModelConfigArgs.NoArgs)

            MultiAppChrome(viewModel, openAllAssets = { startActivity(AllAssetsActivity.newIntent(this))})
        }
    }

    companion object {
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, MultiAppActivity::class.java)
    }
}
