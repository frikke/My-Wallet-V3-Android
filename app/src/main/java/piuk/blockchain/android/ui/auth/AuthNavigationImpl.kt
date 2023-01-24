package piuk.blockchain.android.ui.auth

import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.auth.SecureChannelLoginData
import com.blockchain.home.presentation.navigation.AuthNavigation
import piuk.blockchain.android.ui.auth.newlogin.presentation.AuthNewLoginSheet
import piuk.blockchain.android.ui.home.CredentialsWiper

class AuthNavigationImpl(private val activity: BlockchainActivity?, private val credentialsWiper: CredentialsWiper) :
    AuthNavigation {
    override fun launchAuth(data: SecureChannelLoginData) {
        activity?.showBottomSheet(
            AuthNewLoginSheet.newInstance(
                pubKeyHash = data.pubKeyHash,
                message = data.message,
                originIP = data.originIp,
                originLocation = data.originLocation,
                originBrowser = data.originBrowser
            )
        )
    }

    override fun logout() {
        credentialsWiper.wipe()
    }
}
