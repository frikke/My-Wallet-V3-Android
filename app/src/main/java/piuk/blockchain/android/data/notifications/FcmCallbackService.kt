package piuk.blockchain.android.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.blockchain.analytics.Analytics
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.NotificationsUtil.Companion.ID_BACKGROUND_NOTIFICATION
import com.blockchain.notifications.NotificationsUtil.Companion.ID_BACKGROUND_NOTIFICATION_2FA
import com.blockchain.notifications.NotificationsUtil.Companion.ID_FOREGROUND_NOTIFICATION
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents.Companion.createCampaignPayload
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.emptySubscribe
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.newlogin.domain.model.toArg
import piuk.blockchain.android.ui.auth.newlogin.domain.service.SecureChannelService
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.launcher.LauncherActivity
import timber.log.Timber

class FcmCallbackService : FirebaseMessagingService() {

    private val notificationManager: NotificationManager by inject()
    private val notificationTokenManager: NotificationTokenManager by scopedInject()
    private val analytics: Analytics by inject()
    private val walletPrefs: WalletStatusPrefs by inject()
    private val remoteConfigPrefs: RemoteConfigPrefs by inject()
    private val secureChannelService: SecureChannelService by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val lifecycleObservable: LifecycleObservable by inject()
    private var isAppOnForegrounded = true
    private val deeplinkRedirector: DeeplinkRedirector by scopedInject()
    private val referralPrefs: ReferralPrefs by inject()

    init {
        compositeDisposable += lifecycleObservable.onStateUpdated.subscribe {
            isAppOnForegrounded = it == AppState.FOREGROUNDED
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Send data to analytics
        analytics.logEvent(
            NotificationAnalyticsEvents.PushNotificationReceived(
                createCampaignPayload(remoteMessage.data, remoteMessage.notification?.title)
            )
        )

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: %s", remoteMessage.data)

            // Parse data, emit events
            val payload = NotificationPayload(remoteMessage.data)

            // This payload gets triggered by the cloud function when a remote config gets changed/added
            if (remoteMessage.data["CONFIG_STATE"] == "STALE") {
                remoteConfigPrefs.updateRemoteConfigStaleStatus(isStale = true)
                return
            }

            // Store of we need to display referral success on main screen
            val referralSuccessTitle = payload.referralSuccessTitle
            val referralSuccessBody = payload.referralSuccessBody
            if (referralSuccessTitle != null && referralSuccessBody != null) {
                referralPrefs.referralSuccessTitle = referralSuccessTitle
                referralPrefs.referralSuccessBody = referralSuccessBody
            }

            sendNotification(
                payload = payload,
                foreground = isAppOnForegrounded && walletPrefs.isAppUnlocked
            )
        } else {
            // If there is no data field, provide this default behaviour
            triggerNotification(
                title = remoteMessage.notification?.title,
                body = remoteMessage.notification?.body,
                pendingIntent = PendingIntent.getActivity(
                    applicationContext, 0, Intent(), PendingIntent.FLAG_UPDATE_CURRENT
                ),
                notificationId = ID_BACKGROUND_NOTIFICATION_2FA
            )
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        notificationTokenManager.storeAndUpdateToken(newToken)
        subscribeToRealTimeRemoteConfigUpdates()
    }

    private fun subscribeToRealTimeRemoteConfigUpdates() {
        FirebaseMessaging.getInstance().subscribeToTopic("PUSH_RC")
    }

    /**
     * Redirects the user to the [LauncherActivity] if [foreground] is set to true, otherwise to
     * the [MainActivity] unless it is a new device login, in which case [MainActivity] is
     * going to load the [piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet] .
     *
     * TODO verify if this is true.
     */
    private fun sendNotification(payload: NotificationPayload, foreground: Boolean) {
        compositeDisposable += createIntentForNotification(payload, foreground)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onSuccess = { notifyIntent ->
                    val intent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val notificationId = if (foreground) ID_FOREGROUND_NOTIFICATION else ID_BACKGROUND_NOTIFICATION

                    if (isSecureChannelMessage(payload)) {
                        if (foreground) {
                            startActivity(notifyIntent)
                        } else {
                            triggerNotification(
                                title = getString(R.string.secure_channel_notif_title),
                                body = getString(R.string.secure_channel_notif_summary),
                                pendingIntent = intent,
                                notificationId = notificationId,
                            )
                        }
                    } else if (payload.deeplinkURL != null) {
                        deeplinkRedirector.processDeeplinkURL(
                            Uri.parse(payload.deeplinkURL), payload
                        ).emptySubscribe()
                    } else {
                        triggerNotification(
                            payload.title,
                            payload.body,
                            intent,
                            notificationId
                        )
                    }
                },
                onError = {}
            )
    }

    private fun createIntentForNotification(payload: NotificationPayload, foreground: Boolean): Maybe<Intent> {
        return when {
            isSecureChannelMessage(payload) -> createSecureChannelIntent(payload.payload, foreground)
            foreground -> Maybe.just(
                MainActivity.newIntent(
                    context = applicationContext,
                    intentFromNotification = true,
                    notificationAnalyticsPayload = createCampaignPayload(payload.payload, payload.title)
                )
            )
            else -> Maybe.just(
                LauncherActivity.newInstance(
                    context = applicationContext,
                    intentFromNotification = true,
                    notificationAnalyticsPayload = createCampaignPayload(payload.payload, payload.title)
                )
            )
        }
    }

    private fun isSecureChannelMessage(payload: NotificationPayload) =
        payload.type == NotificationPayload.NotificationType.SECURE_CHANNEL_MESSAGE

    private fun createSecureChannelIntent(payload: Map<String, String?>, foreground: Boolean): Maybe<Intent> {
        val pubKeyHash = payload[NotificationPayload.PUB_KEY_HASH]
            ?: return Maybe.empty()
        val messageRawEncrypted = payload[NotificationPayload.DATA_MESSAGE]
            ?: return Maybe.empty()

        val message = secureChannelService.decryptMessage(pubKeyHash, messageRawEncrypted)
            ?: return Maybe.empty()

        return Maybe.just(
            MainActivity.newIntent(
                context = applicationContext,
                launchAuthFlow = true,
                pubKeyHash = pubKeyHash,
                message = message.toArg(),
                originIp = payload[NotificationPayload.ORIGIN_IP],
                originLocation = payload[NotificationPayload.ORIGIN_COUNTRY],
                originBrowser = payload[NotificationPayload.ORIGIN_BROWSER],
                forcePin = !foreground,
                shouldBeNewTask = foreground
            )
        )
    }

    private fun triggerNotification(
        title: String?,
        body: String?,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {
        NotificationsUtil(
            context = applicationContext,
            notificationManager = notificationManager,
            analytics = analytics
        ).triggerNotification(
            title = title,
            marquee = title,
            text = body,
            pendingIntent = pendingIntent,
            id = notificationId,
            appName = R.string.app_name,
            colorRes = R.color.primary_navy_medium,
            source = "FcmCallbackService"
        )
    }
}
