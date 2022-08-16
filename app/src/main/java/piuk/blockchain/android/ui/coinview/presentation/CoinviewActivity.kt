package piuk.blockchain.android.ui.coinview.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.ui.coinview.presentation.composable.CoinviewScreen

class CoinviewActivity :
    MVIActivity<CoinviewViewState>() {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val viewModel: CoinviewViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CoinviewScreen(

            )

            viewModel.viewCreated(ModelConfigArgs.NoArgs)
        }
    }

    override fun onStateUpdated(state: CoinviewViewState) {
    }
}
