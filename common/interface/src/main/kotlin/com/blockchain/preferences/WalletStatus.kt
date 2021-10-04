package com.blockchain.preferences

import info.blockchain.balance.AssetInfo

interface WalletStatus {
    var lastBackupTime: Long // Seconds since epoch
    val isWalletBackedUp: Boolean

    val isWalletFunded: Boolean
    fun setWalletFunded()

    val hasMadeBitPayTransaction: Boolean
    fun setBitPaySuccess()

    fun setFeeTypeForAsset(asset: AssetInfo, type: Int)
    fun getFeeTypeForAsset(asset: AssetInfo): Int?

    val hasSeenSwapPromo: Boolean
    fun setSeenSwapPromo()

    val resendSmsRetries: Int
    fun setResendSmsRetries(retries: Int)

    var countrySelectedOnSignUp: String
    var stateSelectedOnSignUp: String

    fun clearGeolocationPreferences()

    var email: String

    val hasSeenTradingSwapPromo: Boolean
    fun setSeenTradingSwapPromo()

    var isNewlyCreated: Boolean
    var isRestored: Boolean
    var isLoggedOut: Boolean
}