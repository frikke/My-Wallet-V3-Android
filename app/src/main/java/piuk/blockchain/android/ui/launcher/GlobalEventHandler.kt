package piuk.blockchain.android.ui.launcher

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.extensions.exhaustive
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import info.blockchain.balance.AssetCatalogue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class GlobalEventHandler(
    private val application: Application,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val wcFeatureFlag: IntegratedFeatureFlag,
    private val deeplinkFeatureFlag: IntegratedFeatureFlag,
    private val deeplinkRedirector: DeeplinkRedirector,
    private val assetCatalogue: AssetCatalogue,
    private val coincore: Coincore,
    private val notificationManager: NotificationManager,
    private val analytics: Analytics
) {
    private val compositeDisposable = CompositeDisposable()

    fun init() {
        compositeDisposable.clear()
        compositeDisposable += wcFeatureFlag.enabled.flatMapObservable { enabled ->
            if (enabled) walletConnectServiceAPI.userEvents
            else Observable.empty()
        }.subscribe { event ->
            startTransactionFlowForSigning(event)
        }

        compositeDisposable += deeplinkRedirector.deeplinkEvents.subscribe { deeplinkResult ->
            Timber.d("deeplink: new deeplinkResult")
            navigateToDeeplinkDestination(deeplinkResult)
        }
    }

    private fun navigateToDeeplinkDestination(deeplinkResult: DeepLinkResult.DeepLinkResultSuccess) {
        if (deeplinkResult.notificationPayload != null) {
            triggerNotificationFromDeeplink(deeplinkResult.destination, deeplinkResult.notificationPayload!!)
        } else {
            Timber.d("deeplink: Starting main activity with pending destination")
            application.startActivity(
                MainActivity.newIntent(
                    context = application,
                    pendingDestination = deeplinkResult.destination
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun triggerNotificationFromDeeplink(destination: Destination, notificationPayload: NotificationPayload) {
        var intent: Intent? = null
        when (destination) {
            is Destination.AssetViewDestination -> {
                assetCatalogue.assetInfoFromNetworkTicker(destination.networkTicker)?.let { assetInfo ->
                    intent = CoinViewActivity.newIntent(
                        context = application,
                        asset = assetInfo
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: run {
                    Timber.e("Unable to start CoinViewActivity from deeplink. AssetInfo is null")
                }
            }

            is Destination.AssetBuyDestination -> {
                assetCatalogue.assetInfoFromNetworkTicker(destination.networkTicker)?.let { assetInfo ->
                    intent = SimpleBuyActivity.newIntent(
                        context = application,
                        asset = assetInfo,
                        preselectedAmount = destination.amount
                    )
                } ?: run {
                    Timber.e("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                }
            }

            is Destination.AssetSendDestination -> {
                assetCatalogue.assetInfoFromNetworkTicker(destination.networkTicker)?.let { assetInfo ->
                    coincore.findAccountByAddress(assetInfo, destination.accountAddress).subscribeBy(
                        onSuccess = { account ->
                            if (account is CryptoAccount) {
                                intent = TransactionFlowActivity.newIntent(
                                    context = application,
                                    sourceAccount = account,
                                    action = AssetAction.Send
                                )
                            } else {
                                Timber.e("Unable to start Send from deeplink. Account is not a CryptoAccount")
                            }
                        },
                        onComplete = {
                            Timber.e("Unable to start Send from deeplink. Account not found")
                        },
                        onError = {
                            Timber.e(it)
                        }
                    )
                } ?: run {
                    Timber.e("Unable to start CoinViewActivity from deeplink. AssetInfo is null")
                }

            }

            is Destination.ActivityDestination -> {
                intent =
                    MainActivity.newIntent(
                        context = application,
                        pendingDestination = destination
                    )
            }
        }.exhaustive

        intent?.let { intentFinal ->
            intentFinal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent = PendingIntent.getActivity(
                application,
                0,
                intentFinal,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            pendingIntent?.let { pendingIntentFinal ->
                NotificationsUtil(
                    context = application,
                    notificationManager = notificationManager,
                    analytics = analytics
                ).triggerNotification(
                    title = notificationPayload.title ?: "",
                    marquee = notificationPayload.title ?: "",
                    text = notificationPayload.body ?: "",
                    pendingIntent = pendingIntentFinal,
                    id = NotificationsUtil.ID_BACKGROUND_NOTIFICATION,
                    appName = R.string.app_name,
                    colorRes = R.color.primary_navy_medium
                )
            }
        }
    }

    private fun startTransactionFlowForSigning(event: WalletConnectUserEvent) {
        val intent = TransactionFlowActivity.newIntent(
            application,
            sourceAccount = event.source,
            target = event.target,
            action = AssetAction.Sign
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }
}
