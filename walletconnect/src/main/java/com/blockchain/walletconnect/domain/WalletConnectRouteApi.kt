package com.blockchain.walletconnect.domain

import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.session.WCSession

interface WalletConnectRouteApi {
    fun showConnectedDapps()
    fun showSessionDetails(session: WCSession)
    fun openWebsite(client: WCClient)
}
