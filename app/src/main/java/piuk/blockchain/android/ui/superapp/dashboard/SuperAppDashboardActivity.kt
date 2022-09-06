package piuk.blockchain.android.ui.superapp.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.superapp.dashboard.composable.SuperAppDashboard
import piuk.blockchain.android.ui.superapp2.SuperAppDashboard2

class SuperAppDashboardActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)
            systemUiController.setNavigationBarColor(Color.Transparent)

            SuperAppDashboard2()
        }
    }

    companion object{
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, SuperAppDashboardActivity::class.java)
    }

}
