package piuk.blockchain.android.ui.auth

import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.navigation.AuthNavigation
import piuk.blockchain.android.ui.auth.newlogin.presentation.AuthNewLoginSheet
import piuk.blockchain.android.ui.home.CredentialsWiper

class AuthNavigationImpl(private val activity: BlockchainActivity?, private val credentialsWiper: CredentialsWiper) :
    AuthNavigation {
    override fun launchAuth(bundle: Bundle) {
        activity?.showBottomSheet(
            AuthNewLoginSheet.newInstance(
                pubKeyHash = bundle.getString(AuthNewLoginSheet.PUB_KEY_HASH),
                message = bundle.getParcelable(AuthNewLoginSheet.MESSAGE),
                forcePin = bundle.getBoolean(AuthNewLoginSheet.FORCE_PIN),
                originIP = bundle.getString(AuthNewLoginSheet.ORIGIN_IP),
                originLocation = bundle.getString(AuthNewLoginSheet.ORIGIN_LOCATION),
                originBrowser = bundle.getString(AuthNewLoginSheet.ORIGIN_BROWSER)
            )
        )
    }

    override fun logout() {
        credentialsWiper.wipe()
    }
}
