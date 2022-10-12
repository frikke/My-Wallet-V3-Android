package com.blockchain.notifications

import android.annotation.SuppressLint
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.NotificationPrefs
import com.google.common.base.Optional
import info.blockchain.wallet.payload.WalletPayloadService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class NotificationTokenManager(
    private val notificationService: NotificationService,
    private val walletPayloadService: WalletPayloadService,
    private val prefs: NotificationPrefs,
    private val authPrefs: AuthPrefs,
    private val notificationTokenProvider: NotificationTokenProvider,
    private val remoteLogger: RemoteLogger
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

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun disableNotifications(): Completable {
        prefs.arePushNotificationsEnabled = false
        return revokeAccessToken()
            .observeOn(AndroidSchedulers.mainThread())
    }

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
                remoteLogger.logException(
                    throwable = throwable,
                    logMessage = "Failed to resend the Firebase token for notifications"
                )
            }
    }

    private fun sendFirebaseToken(refreshedToken: String): Completable {
        return if (prefs.arePushNotificationsEnabled && walletPayloadService.initialised) {
            notificationService.sendNotificationToken(
                refreshedToken, walletPayloadService.guid, walletPayloadService.sharedKey
            )
                .retry(2)
                .subscribeOn(Schedulers.io())
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
            notificationService.removeNotificationToken(
                guid = authPrefs.walletGuid,
                sharedKey = authPrefs.sharedKey
            )
        } else {
            Completable.complete()
        }
    }
}
