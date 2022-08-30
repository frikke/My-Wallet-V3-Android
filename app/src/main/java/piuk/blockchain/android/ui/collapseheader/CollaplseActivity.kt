package piuk.blockchain.android.ui.collapseheader

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
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.theme.AppTheme
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity

class CollaplseActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Main()
        }
    }

    companion object{
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, CollaplseActivity::class.java)
    }

}
