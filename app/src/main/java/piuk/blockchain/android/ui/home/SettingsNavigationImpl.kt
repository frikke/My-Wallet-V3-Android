package piuk.blockchain.android.ui.home

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.navigation.HomeLaunch.ACCOUNT_EDIT
import com.blockchain.home.presentation.navigation.QrExpected
import com.blockchain.home.presentation.navigation.SettingsDestination
import com.blockchain.home.presentation.navigation.SettingsNavigation
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.addresses.AddressesActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.settings.SettingsActivity

class SettingsNavigationImpl(private val activity: BlockchainActivity?) : SettingsNavigation {

    private val settingsResultContract =
        activity?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                (
                    it.data?.getSerializableExtra(SettingsActivity.SETTINGS_RESULT_DATA)
                        as? SettingsActivity.Companion.SettingsAction
                    )?.let { action ->
                    startSettingsAction(action)
                }
            }
        }

    override fun settings() {
        settingsResultContract!!.launch(SettingsActivity.newIntent(activity!!))
    }

    override fun settings(settingsDestination: SettingsDestination) {
        activity!!.startActivity(SettingsActivity.newIntent(activity, settingsDestination))
    }

    override fun launchSupportCenter() {
        activity!!.startActivity(SupportCentreActivity.newIntent(activity))
    }

    private fun startSettingsAction(action: SettingsActivity.Companion.SettingsAction) {
        when (action) {
            SettingsActivity.Companion.SettingsAction.Addresses ->
                activity?.startActivityForResult(
                    AddressesActivity.newIntent(activity), ACCOUNT_EDIT
                )
            SettingsActivity.Companion.SettingsAction.Airdrops ->
                activity?.startActivity(AirdropCentreActivity.newIntent(activity))
            SettingsActivity.Companion.SettingsAction.WebLogin ->
                activity?.startActivity(QrScanActivity.newInstance(activity, QrExpected.MAIN_ACTIVITY_QR))
            SettingsActivity.Companion.SettingsAction.Logout -> {}
        }.also {
            activity?.hideLoading()
        }
    }
}
