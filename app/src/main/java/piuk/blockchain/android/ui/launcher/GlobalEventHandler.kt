package piuk.blockchain.android.ui.launcher

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.navigation.DestinationArgs
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.MaybeSubject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivity
import piuk.blockchain.android.ui.home.HomeActivityLauncher
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class GlobalEventHandler(
    private val application: Application,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val deeplinkRedirector: DeeplinkRedirector,
    private val destinationArgs: DestinationArgs,
    private val notificationManager: NotificationManager,
    private val analytics: Analytics,
    private val homeActivityLauncher: HomeActivityLauncher
) {
    private val compositeDisposable = CompositeDisposable()

    fun init() {

        clear()

        compositeDisposable += walletConnectServiceAPI.userEvents.subscribe { event ->
            startTransactionFlowForSigning(event)
        }

        compositeDisposable += walletConnectV2Service.userEvents.distinctUntilChanged().asObservable().subscribe {
            Timber.d("New WalletConnectV2 User Event ${it.javaClass.simpleName}")
            startTransactionFlowForSigning(it)
        }

        compositeDisposable += walletConnectV2Service.dappRedirectEvents
            .distinctUntilChanged()
            .asObservable()
            .subscribe {
                Timber.d("New WalletConnectV2 Dapp Redirect Event: $it")
                openUri(it.toUri())
            }

        compositeDisposable += deeplinkRedirector.deeplinkEvents
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { deeplinkResult ->
                navigateToDeeplinkDestination(deeplinkResult)
            }
    }

    fun clear() {
        compositeDisposable.clear()
    }

    private fun navigateToDeeplinkDestination(deeplinkResult: DeepLinkResult) {
        // this is a known v2 deeplink that should be handled with the newer approach
        if (deeplinkResult is DeepLinkResult.DeepLinkResultSuccess) {
            if (deeplinkResult.notificationPayload != null) {
                Timber.d("deeplink: triggering notification with deeplink")
                triggerNotificationFromDeeplink(deeplinkResult.destination, deeplinkResult.notificationPayload!!)
            } else {
                Timber.d("deeplink: Starting main activity with pending destination")
                application.startActivity(
                    homeActivityLauncher.newIntent(
                        context = application,
                        pendingDestination = deeplinkResult.destination
                    )
                )
            }
        }
    }

    private fun buildNotificationIntentFromDeeplink(destination: Destination): Maybe<Intent> {
        val subject = MaybeSubject.create<Intent>()
        when (destination) {
            is Destination.AssetViewDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    CoinViewActivity.newIntent(
                        context = application,
                        asset = assetInfo,
                        recurringBuyId = destination.recurringBuyId,
                        originScreen = LaunchOrigin.NOTIFICATION.name
                    )
                } ?: run {
                    subject.onError(
                        Exception("Unable to start CoinViewActivity from deeplink. AssetInfo is null")
                    )
                }
            }

            is Destination.AssetBuyDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    subject.onSuccess(
                        SimpleBuyActivity.newIntent(
                            context = application,
                            asset = assetInfo,
                            preselectedAmount = destination.amount
                        )
                    )
                } ?: run {
                    subject.onError(
                        Exception("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                    )
                }
            }

            is Destination.AssetSendDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    destinationArgs.getSendSourceCryptoAccount(assetInfo, destination.accountAddress).subscribeBy(
                        onSuccess = { account ->
                            subject.onSuccess(
                                TransactionFlowActivity.newIntent(
                                    context = application,
                                    sourceAccount = account,
                                    action = AssetAction.Send
                                )
                            )
                        },
                        onError = {
                            subject.onError(it)
                        }
                    )
                } ?: run {
                    subject.onError(
                        Exception("Unable to start Send flow from deeplink. AssetInfo is null")
                    )
                }
            }

            is Destination.ActivityDestination -> {
                subject.onSuccess(
                    homeActivityLauncher.newIntent(
                        context = application,
                        pendingDestination = destination
                    )
                )
            }

            else -> subject.onError(
                Exception("Deeplink destination not recognized")
            )
        }

        return subject
    }

    private fun triggerNotificationFromDeeplink(destination: Destination, notificationPayload: NotificationPayload) {
        buildNotificationIntentFromDeeplink(destination).subscribeBy(
            onSuccess = { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pendingIntent = PendingIntent.getActivity(
                    application,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                pendingIntent?.let { pendingIntentFinal ->
                    NotificationsUtil(
                        context = application,
                        notificationManager = notificationManager,
                        analytics = analytics
                    ).triggerNotification(
                        title = notificationPayload.title,
                        marquee = notificationPayload.title,
                        text = notificationPayload.body,
                        pendingIntent = pendingIntentFinal,
                        id = NotificationsUtil.ID_BACKGROUND_NOTIFICATION,
                        appName = com.blockchain.stringResources.R.string.app_name,
                        colorRes = com.blockchain.common.R.color.primary_navy_medium,
                        source = "GlobalEventHandler: $destination"
                    )
                }
            },
            onError = {
                Timber.e(it)
            }
        )
    }

    private fun startTransactionFlowForSigning(event: WalletConnectUserEvent) {
        val intent = TransactionFlowActivity.newIntent(
            application,
            sourceAccount = event.source,
            target = event.target,
            action = when (event) {
                is WalletConnectUserEvent.SignTransaction,
                is WalletConnectUserEvent.SignMessage -> AssetAction.Sign
                is WalletConnectUserEvent.SendTransaction -> AssetAction.Send
            }
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun openUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }
}
