package com.blockchain.notifications

import android.annotation.SuppressLint
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.NotificationPrefs
import com.google.common.base.Optional
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class NotificationTokenManager(
    private val notificationService: NotificationService,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: NotificationPrefs,
    private val authPrefs: AuthPrefs,
    private val notificationTokenProvider: NotificationTokenProvider,
    private val remoteLogger: RemoteLogger
) {
    private val coroutineDispatcher = Schedulers.io().asCoroutineDispatcher()

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by [InstanceIdService]
     *
     * @return The Firebase token
     */
    private val storedFirebaseToken: Single<Optional<String>>
        get() {
            return rxSingle(coroutineDispatcher) { prefs.getFirebaseToken() }
                .flatMap { storedToken ->
                    if (storedToken.isNotEmpty()) {
                        Single.just(Optional.of(storedToken))
                    } else {
                        notificationTokenProvider.notificationToken().doOnSuccess {
                            rxCompletable(coroutineDispatcher) { prefs.setFirebaseToken(it) }.subscribe()
                        }.flatMap {
                            Single.just(Optional.of(it))
                        }
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
        rxCompletable(coroutineDispatcher) { prefs.setFirebaseToken(token) }
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    if (token.isNotEmpty()) {
                        sendFirebaseToken(token)
                            .subscribeOn(Schedulers.io())
                            .subscribe({ /*no-op*/ }, { Timber.e(it) })
                    }
                },
                { Timber.e(it) }
            )
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun disableNotifications(): Completable =
        rxCompletable(coroutineDispatcher) { prefs.setPushNotificationsEnabled(false) }
            .mergeWith(revokeAccessToken())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun revokeAccessToken(): Completable =
        notificationTokenProvider.deleteToken()
            .then { removeNotificationToken() }
            .onErrorComplete()
            .doOnComplete { this.clearStoredToken() }
            .subscribeOn(Schedulers.io())

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    fun enableNotifications(): Completable =
        rxCompletable(coroutineDispatcher) { prefs.setPushNotificationsEnabled(true) }
            .mergeWith(resendNotificationToken())

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
                remoteLogger.logException(
                    throwable = throwable,
                    logMessage = "Failed to resend the Firebase token for notifications"
                )
            }
    }

    private fun sendFirebaseToken(refreshedToken: String): Completable {
        return rxSingle(coroutineDispatcher) { prefs.arePushNotificationsEnabled() }
            .flatMapCompletable { pushNotificationsEnabled ->
                if (pushNotificationsEnabled && payloadDataManager.initialised) {
                    notificationService.sendNotificationToken(
                        refreshedToken, payloadDataManager.guid, payloadDataManager.sharedKey
                    )
                        .retry(2)
                        .subscribeOn(Schedulers.io())
                } else {
                    Completable.complete()
                }
            }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private fun clearStoredToken() {
        rxCompletable(coroutineDispatcher) { prefs.setFirebaseToken("") }.subscribe()
    }

    /**
     * Removes the stored token from back end
     */
    private fun removeNotificationToken() =
        rxSingle(coroutineDispatcher) { prefs.getFirebaseToken() }.flatMapCompletable { token ->
            if (token.isNotEmpty()) {
                notificationService.removeNotificationToken(
                    guid = authPrefs.walletGuid,
                    sharedKey = authPrefs.sharedKey
                )
            } else {
                Completable.complete()
            }
        }
}
