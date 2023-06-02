package com.blockchain.notifications

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Completable

class NotificationService(private val walletApi: WalletApi, private val notificationStorage: NotificationStorage) {
    /**
     * Sends the updated Firebase token to the server along with the GUID and Shared Key
     *
     * @param token A Firebase notification token
     * @param guid The user's GUID
     * @param sharedKey The user's shared key
     * @return A [Completable], ie an Observable type object specifically for methods
     * returning void.
     */
    fun sendNotificationToken(token: String, guid: String, sharedKey: String): Completable =
        notificationStorage.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
                TokenCredentials(
                    token, sharedKey, guid
                )
            )
        ).asSingle().ignoreElement()

    /**
     * Removes the Firebase token from the server along with the GUID and Shared Key
     *
     * @param guid wallet GUID
     * @param sharedKey wallet PIN shared key
     * @return A [Completable], ie an Observable type object specifically for methods
     * returning void.
     */
    fun removeNotificationToken(guid: String, sharedKey: String): Completable =
        walletApi.removeFirebaseNotificationToken(guid, sharedKey)
}
