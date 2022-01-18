package com.blockchain.walletconnect.data

import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession

class FakeWalletConnectService : WalletConnectServiceAPI {

    override fun attemptToConnect(url: String) {
    }

    override fun connectToApprovedSessions() {
    }

    override fun acceptConnection(session: WalletConnectSession) {
    }

    override fun denyConnection(session: WalletConnectSession) {
    }
}
