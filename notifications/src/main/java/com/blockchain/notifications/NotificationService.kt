package com.blockchain.notifications

import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Completable

class NotificationService(private val walletApi: WalletApi) {
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
        Completable.fromObservable(
            walletApi.updateFirebaseNotificationToken(token, guid, sharedKey)
        )

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