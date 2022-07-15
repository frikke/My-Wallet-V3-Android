package com.blockchain.deeplinking.processor

import android.net.Uri
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.notifications.models.NotificationPayload
import io.reactivex.rxjava3.core.Single
import timber.log.Timber

class DeeplinkProcessorV2 {

    fun process(deeplinkUri: Uri, payload: NotificationPayload? = null): Single<DeepLinkResult> {

        Timber.d("deeplink uri: %s", deeplinkUri.path)

        return when (deeplinkUri.path) {
            ASSET_URL -> {
                val networkTicker = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: openAsset with args $networkTicker")

                if (!networkTicker.isNullOrEmpty()) {
                    val destination = Destination.AssetViewDestination(networkTicker)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
            }
            BUY_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                val amount = getAmount(deeplinkUri)
                Timber.d("deeplink: AssetBuy with args $code, $amount")

                if (!code.isNullOrEmpty() && !amount.isNullOrEmpty()) {
                    val destination = Destination.AssetBuyDestination(code, amount)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            SEND_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                val amount = getAmount(deeplinkUri)
                val address = getAddress(deeplinkUri)
                Timber.d("deeplink: AssetSend with args $code, $amount, $address") // TODO can we log address??
                if (!code.isNullOrEmpty() && !amount.isNullOrEmpty() && !address.isNullOrEmpty()) {
                    val destination = Destination.AssetSendDestination(code, amount, address)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            ACTIVITY_URL -> {
                // Todo add filter parameter to destination
                // val filter = getFilter(deeplinkUri)
                Timber.d("deeplink: Activity")

                val destination = Destination.ActivityDestination()
                Single.just(DeepLinkResult.DeepLinkResultSuccess(destination = destination, payload))
            }
            DIFFERENT_CARD_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Different card with code: $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetEnterAmountLinkCardDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            DIFFERENT_PAYMENT_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Different payment with code: $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetEnterAmountNewMethodDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            ENTER_AMOUNT_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Enter Amount with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetEnterAmountDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            CUSTOMER_SUPPORT_URL -> {
                Timber.d("deeplink: Customer support")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.CustomerSupportDestination,
                        notificationPayload = payload
                    )
                )
            }
            KYC_URL -> {
                Timber.d("deeplink: KYC")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.StartKycDestination,
                        notificationPayload = payload
                    )
                )
            }
            REFERRAL_URL -> {
                Timber.d("deeplink: Referral")

                val destination = Destination.ReferralDestination
                Single.just(DeepLinkResult.DeepLinkResultSuccess(destination = destination, payload))
            }
            EXTERNAL_LINK_URL -> {
                val url = getUrl(deeplinkUri)
                Timber.d("deeplink: External link with URL $url")

                if (!url.isNullOrEmpty()) {
                    val destination = Destination.ExternalLinkDestination(url)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultFailed)
                }
            }
            DASHBOARD_URL -> {
                Timber.d("deeplink: Dashboard")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.DashboardDestination,
                        notificationPayload = payload
                    )
                )
            }
            else -> Single.just(DeepLinkResult.DeepLinkResultFailed)
        }
    }

    companion object {

        private const val APP_URL = "/app"
        private const val TRANSACTION_URL = "$APP_URL/transaction"

        const val ASSET_URL = "$APP_URL/asset"
        const val BUY_URL = "$ASSET_URL/buy"
        const val SEND_URL = "$ASSET_URL/send"

        const val DIFFERENT_CARD_URL = "$TRANSACTION_URL/try/different/card"
        const val DIFFERENT_PAYMENT_URL = "$TRANSACTION_URL/try/different/payment_method"
        const val ENTER_AMOUNT_URL = "$TRANSACTION_URL/back/to/enter_amount"

        const val CUSTOMER_SUPPORT_URL = "$APP_URL/contact/customer/support"
        const val KYC_URL = "$APP_URL/kyc"
        const val ACTIVITY_URL = "$APP_URL/activity"
        const val REFERRAL_URL = "$APP_URL/referral"
        const val EXTERNAL_LINK_URL = "$APP_URL/external/link"
        const val DASHBOARD_URL = "$APP_URL/go/to/dashboard"

        const val PARAMETER_CODE = "code"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_ADDRESS = "address"
        const val PARAMETER_FILTER = "filter"
        const val PARAMETER_URL = "url"
    }

    private fun getAssetNetworkTicker(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_CODE)

    private fun getAmount(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_AMOUNT)

    private fun getAddress(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_ADDRESS)

    private fun getFilter(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_FILTER)

    private fun getUrl(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_URL)
}

sealed class DeepLinkResult {
    data class DeepLinkResultSuccess(
        val destination: Destination,
        val notificationPayload: NotificationPayload?
    ) : DeepLinkResult()
    object DeepLinkResultFailed : DeepLinkResult()
}
