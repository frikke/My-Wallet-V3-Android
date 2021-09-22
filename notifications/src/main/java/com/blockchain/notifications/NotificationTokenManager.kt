package com.blockchain.notifications

import android.annotation.SuppressLint
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.NotificationPrefs
import com.google.common.base.Optional
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

class NotificationTokenManager(
    private val notificationService: NotificationService,
    private val payloadManager: PayloadManager,
    private val prefs: NotificationPrefs,
    private val notificationTokenProvider: NotificationTokenProvider,
    private val rxBus: RxBus,
    private val crashLogger: CrashLogger
) {

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by [InstanceIdService]
     *
     * @return The Firebase token
     */
    private val storedFirebaseToken: Single<Optional<String>>
        get() {
            val storedToken = prefs.firebaseToken
            return if (storedToken.isNotEmpty()) {
                Single.just(Optional.of(storedToken))
            } else {
                notificationTokenProvider.notificationToken().doOnSuccess {
                    prefs.firebaseToken = it
                }.flatMap {
                    Single.just(Optional.of(it))
                }
            }
        }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully.
     *
     * @param token A Firebase access token
     */
    @SuppressLint("CheckResult")
    fun storeAndUpdateToken(token: String) {
        prefs.firebaseToken = token
        if (token.isNotEmpty()) {
            sendFirebaseToken(token)
                .subscribeOn(Schedulers.io())
                .subscribe({ /*no-op*/ }, { Timber.e(it) })
        }
    }

    @SuppressLint("CheckResult")
    fun registerAuthEvent() {
        val loginObservable = rxBus.register(AuthEvent::class.java)

        loginObservable
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { authEvent ->
                if (authEvent == AuthEvent.FORGET) {
                    revokeAccessToken()
                } else {
                    Completable.complete()
                }
            }
            .subscribe({
                // no-op
            }, { Timber.e(it) })
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun disableNotifications(): Completable {
        prefs.arePushNotificationsEnabled = false
        return revokeAccessToken()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    private fun revokeAccessToken(): Completable =
        notificationTokenProvider.deleteToken()
            .andThen(removeNotificationToken())
            .doOnComplete { this.clearStoredToken() }
            .subscribeOn(Schedulers.io())

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    fun enableNotifications(): Completable {
        prefs.arePushNotificationsEnabled = true
        return resendNotificationToken()
    }

    /**
     * If no stored notification token exists, it will be refreshed
     * and will be handled appropriately by FcmCallbackService
     */
    fun resendNotificationToken(): Completable {
        return storedFirebaseToken
            .flatMapCompletable { optional ->
                if (optional.isPresent) {
                    sendFirebaseToken(optional.get())
                } else {
                    Completable.complete()
                }
            }
            .doOnError { throwable ->
                crashLogger.logException(
                    throwable = throwable,
                    logMsg = "Failed to resend the Firebase token for notifications"
                )
            }
    }

    private fun sendFirebaseToken(refreshedToken: String): Completable {
        return if (prefs.arePushNotificationsEnabled && payloadManager.payload != null) {
            val payload = payloadManager.payload
            payload?.let {
                notificationService.sendNotificationToken(refreshedToken, it.guid, it.sharedKey)
                    .retry(2)
                    .subscribeOn(Schedulers.io())
            } ?: Completable.complete()
        } else {
            Completable.complete()
        }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private fun clearStoredToken() {
        prefs.firebaseToken = ""
    }

    /**
     * Removes the stored token from back end
     */
    private fun removeNotificationToken(): Completable {
        val token = prefs.firebaseToken

        return if (token.isNotEmpty()) {
            val payload = payloadManager.payload
            payload?.let {
                notificationService.removeNotificationToken(it.guid, it.sharedKey)
            } ?: Completable.complete()
        } else {
            Completable.complete()
        }
    }
}
