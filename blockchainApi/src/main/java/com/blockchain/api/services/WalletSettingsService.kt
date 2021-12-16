package com.blockchain.api.services

import com.blockchain.api.wallet.WalletApi
import com.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class WalletSettingsService internal constructor(
    private val api: WalletApi,
    private val apiCode: String
) {
    fun fetchWalletSettings(
        guid: String,
        sharedKey: String
    ): Single<UserInfoSettings> = api.fetchSettings(guid = guid, sharedKey = sharedKey, apiCode = apiCode)
        .map { it.toUserInfoSettings() }

    fun triggerAlert(
        guid: String,
        sharedKey: String
    ): Completable = api.triggerAlert(guid = guid, sharedKey = sharedKey)

    fun triggerOnChainTransaction(
        guid: String,
        sharedKey: String,
        currency: String,
        amount: String
    ): Completable {
        return api.triggerOnChainTransaction(
            guid = guid,
            sharedKey = sharedKey,
            currency = currency,
            amount = amount,
            apiCode = apiCode
        )
    }

    private fun WalletSettingsDto.toUserInfoSettings() = UserInfoSettings(
        this.email,
        this.emailVerified != 0,
        this.smsNumber,
        this.smsVerified != 0
    )

    data class UserInfoSettings(
        val email: String? = null,
        val emailVerified: Boolean = false,
        val mobile: String? = null,
        val mobileVerified: Boolean = false
    )
}
