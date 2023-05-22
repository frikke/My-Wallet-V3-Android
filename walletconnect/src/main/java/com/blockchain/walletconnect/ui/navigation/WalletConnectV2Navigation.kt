package com.blockchain.walletconnect.ui.navigation

interface WalletConnectV2Navigation {

    fun launchWalletConnectV2()
    fun approveOrRejectSession(dappName: String, dappDescription: String, dappLogoUrl: String)
    fun sessionApproveSuccess(dappName: String, dappLogoUrl: String)
    fun sessionApproveFailed()
    fun sessionRejected(dappName: String, dappLogoUrl: String)
    fun sessionUnsupported(dappName: String, dappLogoUrl: String)
}
