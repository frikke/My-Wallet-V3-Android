package piuk.blockchain.android.ui.educational.walletmodes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.ui.educational.walletmodes.screens.EducationalWalletModeScreen
import piuk.blockchain.android.ui.home.MainActivity

class EducationalWalletModeActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean = true

    val viewModel: EducationalWalletModeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EducationalWalletModeScreen(
                onClick = {
                    viewModel.markAsSeen()
                    launchMainActivity()
                }
            )
        }
    }

    private fun launchMainActivity() {
        startActivity(
            MainActivity.newIntent(
                context = this,
                intentData = intent.getStringExtra(MAIN_ACTIVITY_DATA),
                shouldLaunchUiTour = intent.getBooleanExtra(START_UI_TOUR, false),
                shouldBeNewTask = true
            )
        )
        finish()
    }

    companion object {
        private const val MAIN_ACTIVITY_DATA = "MAIN_ACTIVITY_DATA"
        private const val START_UI_TOUR = "START_UI_TOUR"

        fun newIntent(
            context: Context,
            data: String?,
            shouldLaunchUiTour: Boolean
        ): Intent = Intent(context, EducationalWalletModeActivity::class.java).apply {
            putExtra(MAIN_ACTIVITY_DATA, data)
            putExtra(START_UI_TOUR, shouldLaunchUiTour)
        }
    }
}
