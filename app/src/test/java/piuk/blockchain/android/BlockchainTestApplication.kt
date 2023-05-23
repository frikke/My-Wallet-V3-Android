package piuk.blockchain.android

import android.annotation.SuppressLint
import android.app.Application
import com.google.firebase.FirebaseApp
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.android.relay.NetworkClientTimeout
import com.walletconnect.android.relay.RelayConnectionInterface
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@SuppressLint("Registered")
class BlockchainTestApplication : BlockchainApplication() {
    override fun onCreate() {
        FirebaseApp.initializeApp(this)
        super.onCreate()
    }

    override fun checkSecurityProviderAndPatchIfNeeded() {
        // No-op
    }
}

@Implements(CoreClient::class)
class FakeCoreClient {
    @Implementation
    fun initialize(
        metaData: Core.Model.AppMetaData,
        relayServerUrl: String,
        connectionType: ConnectionType,
        application: Application,
        relay: RelayConnectionInterface? = null,
        keyServerUrl: String? = null,
        networkClientTimeout: NetworkClientTimeout? = null,
        onError: (Core.Model.Error) -> Unit
    ) { }
}

@Implements(Web3Wallet::class)
class FakeWeb3Wallet {
    @Implementation
    fun initialize(params: Wallet.Params.Init, onSuccess: () -> Unit = {}, onError: (Wallet.Model.Error) -> Unit) { }
}
