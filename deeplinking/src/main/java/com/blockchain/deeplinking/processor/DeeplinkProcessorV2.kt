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
                    val recurringBuyId = getRecurringBuyId(deeplinkUri)
                    val destination = Destination.AssetViewDestination(networkTicker, recurringBuyId)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            BUY_URL -> {
                val cryptoTicker = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: AssetBuy with args $cryptoTicker")

                if (!cryptoTicker.isNullOrEmpty()) {
                    val amount = getAmount(deeplinkUri)
                    val fiatTicker = getFiatTicker(deeplinkUri)
                    val destination = Destination.AssetBuyDestination(
                        networkTicker = cryptoTicker,
                        amount = amount,
                        fiatTicker = fiatTicker
                    )

                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
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
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            SWAP_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Swap with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetSwapDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            SELL_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Sell with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetSellDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            RECEIVE_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Receive with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.AssetReceiveDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            REWARDS_DEPOSIT_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Rewards deposit with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.RewardsDepositDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            REWARDS_SUMMARY_URL -> {
                val code = getAssetNetworkTicker(deeplinkUri)
                Timber.d("deeplink: Rewards summary with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.RewardsSummaryDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            FIAT_DEPOSIT_URL -> {
                val code = getFiatTicker(deeplinkUri)
                Timber.d("deeplink: fiat deposit with args $code")

                if (!code.isNullOrEmpty()) {
                    val destination = Destination.FiatDepositDestination(code)
                    Single.just(
                        DeepLinkResult.DeepLinkResultSuccess(
                            destination = destination,
                            notificationPayload = payload
                        )
                    )
                } else {
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
                }
            }
            SETTINGS_ADD_CARD -> {
                Timber.d("deeplink: add card")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.SettingsAddCardDestination,
                        notificationPayload = payload
                    )
                )
            }
            SETTINGS_ADD_BANK -> {
                Timber.d("deeplink: add bank")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.SettingsAddBankDestination,
                        notificationPayload = payload
                    )
                )
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
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
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
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
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
                    Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
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
            DASHBOARD_URL -> {
                Timber.d("deeplink: Dashboard")

                Single.just(
                    DeepLinkResult.DeepLinkResultSuccess(
                        destination = Destination.DashboardDestination,
                        notificationPayload = payload
                    )
                )
            }
            else -> {
                Single.just(DeepLinkResult.DeepLinkResultUnknownLink(deeplinkUri))
            }
        }
    }

    companion object {

        private const val APP_URL = "/app"
        private const val TRANSACTION_URL = "$APP_URL/transaction"

        const val ASSET_URL = "$APP_URL/asset"
        const val BUY_URL = "$ASSET_URL/buy"
        const val SEND_URL = "$ASSET_URL/send"
        const val SWAP_URL = "$ASSET_URL/swap"
        const val SELL_URL = "$ASSET_URL/sell"
        const val RECEIVE_URL = "$ASSET_URL/receive"
        private const val REWARDS_URL = "$ASSET_URL/rewards"
        const val REWARDS_DEPOSIT_URL = "$REWARDS_URL/deposit"
        const val REWARDS_SUMMARY_URL = "$REWARDS_URL/summary"

        const val FIAT_DEPOSIT_URL = "$APP_URL/fiat/deposit"
        private const val SETTINGS_URL = "$APP_URL/settings"
        const val SETTINGS_ADD_CARD = "$SETTINGS_URL/add/card"
        const val SETTINGS_ADD_BANK = "$SETTINGS_URL/add/bank"

        const val DIFFERENT_CARD_URL = "$TRANSACTION_URL/try/different/card"
        const val DIFFERENT_PAYMENT_URL = "$TRANSACTION_URL/try/different/payment_method"
        const val ENTER_AMOUNT_URL = "$TRANSACTION_URL/back/to/enter_amount"

        const val CUSTOMER_SUPPORT_URL = "$APP_URL/contact/customer/support"
        const val KYC_URL = "$APP_URL/kyc"
        const val ACTIVITY_URL = "$APP_URL/activity"
        const val REFERRAL_URL = "$APP_URL/referral"
        const val DASHBOARD_URL = "$APP_URL/go/to/dashboard"

        const val PARAMETER_RECURRING_BUY_ID = "recurring_buy_id"
        const val PARAMETER_CODE = "code"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_ADDRESS = "address"
        const val FIAT_TICKER = "currency"
    }

    private fun getRecurringBuyId(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_RECURRING_BUY_ID)

    private fun getAssetNetworkTicker(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_CODE)

    private fun getAmount(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_AMOUNT)

    private fun getFiatTicker(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(FIAT_TICKER)

    private fun getAddress(deeplinkUri: Uri): String? =
        deeplinkUri.getQueryParameter(PARAMETER_ADDRESS)
}

sealed class DeepLinkResult {
    data class DeepLinkResultSuccess(
        val destination: Destination,
        val notificationPayload: NotificationPayload?
    ) : DeepLinkResult()

    data class DeepLinkResultUnknownLink(val uri: Uri? = null) : DeepLinkResult()
}
