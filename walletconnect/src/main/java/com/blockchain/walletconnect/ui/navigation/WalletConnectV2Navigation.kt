package com.blockchain.walletconnect.ui.navigation

interface WalletConnectV2Navigation {

    fun launchWalletConnectV2()
    fun approveOrRejectSession(sessionId: String, walletAddress: String)
    fun approveSession()
    fun rejectSession()
    fun sessionUnsupported(dappName: String, dappLogoUrl: String)
}
