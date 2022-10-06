package com.blockchain.blockchaincard.googlewallet.manager

import android.app.Activity
import android.content.Context
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletUserAddress
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayClient
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tapandpay.issuer.UserAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import timber.log.Timber

class GoogleWalletManager(
    context: Context
) {

    private val tapAndPayClient: TapAndPayClient by lazy {
        TapAndPay.getClient(context)
    }

    suspend fun getWalletId(): String =
        suspendCoroutine { continuation ->
            val task = tapAndPayClient.activeWalletId
            task.addOnCompleteListener { completedTask ->
                try {
                    completedTask.getResult(Exception::class.java)?.let { continuation.resume(it) }
                } catch (exception: Exception) {
                    Timber.w(exception, "getActiveWallet failed")
                    continuation.resume("")
                }
            }
        }

    suspend fun getStableHardwareId(): String =
        suspendCoroutine { continuation ->
            val task = tapAndPayClient.stableHardwareId
            task.addOnCompleteListener { completedTask ->
                try {
                    completedTask.getResult(Exception::class.java)?.let { continuation.resume(it) }
                } catch (exception: Exception) {
                    Timber.w(exception, "getActiveWallet failed")
                    continuation.resume("")
                }
            }
        }

    suspend fun getTokenizationStatus(last4Digits: String): Boolean {
        val isTokenizedRequest = IsTokenizedRequest.Builder()
            .setNetwork(TapAndPay.CARD_NETWORK_VISA)
            .setTokenServiceProvider(TapAndPay.TOKEN_PROVIDER_VISA)
            .setIdentifier(last4Digits)
            .build()

        return suspendCoroutine { continuation ->
            val task = tapAndPayClient.isTokenized(isTokenizedRequest)
            task.addOnCompleteListener { completedTask ->
                try {
                    completedTask.getResult(Exception::class.java)?.let { continuation.resume(it) }
                } catch (exception: Exception) {
                    Timber.w(exception, "getTokenizationStatus failed")
                    continuation.resume(false)
                }
            }
        }
    }

    fun pushTokenizeRequest(
        activity: Activity,
        tokenizeRequest: BlockchainCardGoogleWalletPushTokenizeData,
        requestCode: Int
    ) {
        tapAndPayClient.pushTokenize(
            activity,
            buildTokenizeRequest(tokenizeRequest),
            requestCode
        )
    }

    private fun buildTokenizeRequest(
        pushTokenizeData: BlockchainCardGoogleWalletPushTokenizeData
    ): PushTokenizeRequest {
        return PushTokenizeRequest.Builder()
            .setOpaquePaymentCard(pushTokenizeData.opaquePaymentCard.toByteArray())
            .setNetwork(pushTokenizeData.network.tokenizationNetworkToInt())
            .setTokenServiceProvider(pushTokenizeData.tokenServiceProvider.tokenServiceProviderToInt())
            .setDisplayName(pushTokenizeData.displayName)
            .setLastDigits(pushTokenizeData.last4)
            .setUserAddress(pushTokenizeData.googleWalletUserAddress.buildUserAddress())
            .build()
    }

    private fun String.tokenizationNetworkToInt(): Int =
        when {
            equals("Amex", true) -> TapAndPay.CARD_NETWORK_AMEX
            equals("Discover", true) -> TapAndPay.CARD_NETWORK_DISCOVER
            equals("Mastercard", true) -> TapAndPay.CARD_NETWORK_MASTERCARD
            equals("Visa", true) -> TapAndPay.CARD_NETWORK_VISA
            equals("Interac", true) -> TapAndPay.CARD_NETWORK_INTERAC
            equals("Private_label", true) -> TapAndPay.CARD_NETWORK_PRIVATE_LABEL
            equals("Eftpos", true) -> TapAndPay.CARD_NETWORK_EFTPOS
            equals("Maestro", true) -> TapAndPay.CARD_NETWORK_MAESTRO
            equals("Id", true) -> TapAndPay.CARD_NETWORK_ID
            equals("Quicpay", true) -> TapAndPay.CARD_NETWORK_QUICPAY
            equals("Jcb", true) -> TapAndPay.CARD_NETWORK_JCB
            equals("Elo", true) -> TapAndPay.CARD_NETWORK_ELO
            equals("Mir", true) -> TapAndPay.CARD_NETWORK_MIR
            else -> -1
        }

    private fun String.tokenServiceProviderToInt(): Int =
        when (this) {
            "TOKEN_PROVIDER_VISA" -> TapAndPay.TOKEN_PROVIDER_VISA
            "TOKEN_PROVIDER_MASTERCARD" -> TapAndPay.TOKEN_PROVIDER_MASTERCARD
            "TOKEN_PROVIDER_AMEX" -> TapAndPay.TOKEN_PROVIDER_AMEX
            "TOKEN_PROVIDER_DISCOVER" -> TapAndPay.TOKEN_PROVIDER_DISCOVER
            "TOKEN_PROVIDER_INTERAC" -> TapAndPay.TOKEN_PROVIDER_INTERAC
            "TOKEN_PROVIDER_QUICPAY" -> TapAndPay.TOKEN_PROVIDER_EFTPOS
            "TOKEN_PROVIDER_OBERTHUR" -> TapAndPay.TOKEN_PROVIDER_OBERTHUR
            "TOKEN_PROVIDER_PAYPAL" -> TapAndPay.TOKEN_PROVIDER_PAYPAL
            "TOKEN_PROVIDER_JCB" -> TapAndPay.TOKEN_PROVIDER_JCB
            "TOKEN_PROVIDER_GEMALTO" -> TapAndPay.TOKEN_PROVIDER_GEMALTO
            else -> -1
        }

    private fun BlockchainCardGoogleWalletUserAddress.buildUserAddress(): UserAddress =
        UserAddress.newBuilder()
            .setName(name)
            .setAddress1(address1)
            .setLocality(city)
            .setAdministrativeArea(stateCode)
            .setCountryCode(countryCode)
            .setPostalCode(postalCode)
            .setPhoneNumber(phone)
            .build()
}
