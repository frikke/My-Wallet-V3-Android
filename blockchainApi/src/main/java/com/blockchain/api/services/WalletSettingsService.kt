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
        email = this.email,
        emailVerified = this.emailVerified != 0,
        mobileWithPrefix = this.smsNumber,
        mobileVerified = this.smsVerified != 0,
        authType = this.authType,
        dialCode = this.dialCode
    )

    data class UserInfoSettings(
        val email: String? = null,
        val emailVerified: Boolean = false,
        val mobileWithPrefix: String? = null,
        val mobileVerified: Boolean = false,
        val authType: Int = 0,
        val dialCode: String = "+1"
    ) {
        val mobileNoPrefix: String
            get() = mobileWithPrefix?.drop(dialCode.length.plus(1))?.filterNot { it.isWhitespace() }.orEmpty()
    }
}
