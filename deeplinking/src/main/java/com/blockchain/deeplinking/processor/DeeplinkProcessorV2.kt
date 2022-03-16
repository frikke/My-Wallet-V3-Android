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

            ACTIVITY_URL -> {

                // Todo add filter parameter to destination
                // val filter = getFilter(deeplinkUri)
                Timber.d("deeplink: Activity")

                val destination = Destination.ActivityDestination()
                Single.just(DeepLinkResult.DeepLinkResultSuccess(destination = destination, payload))
            }
            else -> Single.just(DeepLinkResult.DeepLinkResultFailed)
        }
    }

    companion object {

        const val APP_URL = "/app"

        const val ASSET_URL = "$APP_URL/asset"
        const val BUY_URL = "$ASSET_URL/buy"

        const val ACTIVITY_URL = "$APP_URL/activity"

        const val PARAMETER_CODE = "code"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_FILTER = "filter"
    }

    private fun getAssetNetworkTicker(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_CODE)

    private fun getAmount(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_AMOUNT)

    private fun getFilter(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_FILTER)
}

sealed class DeepLinkResult {
    data class DeepLinkResultSuccess(
        val destination: Destination,
        val notificationPayload: NotificationPayload?
    ) : DeepLinkResult()
    object DeepLinkResultFailed : DeepLinkResult()
}
