package com.blockchain.notifications

import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface NotificationTokenProvider {
    fun notificationToken(): Single<String>
    fun deleteToken(): Completable
}

internal class FirebaseNotificationTokenProvider : NotificationTokenProvider {
    override fun notificationToken(): Single<String> {
        return Single.create { subscriber ->
            FirebaseMessaging.getInstance().token.addOnSuccessListener { newToken ->
                subscriber.onSuccess(newToken)
            }
            FirebaseMessaging.getInstance().token.addOnFailureListener {
                if (!subscriber.isDisposed)
                    subscriber.onError(it)
            }
        }
    }

    override fun deleteToken(): Completable =
        Completable.create { emitter ->
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                emitter.onComplete()
            }
            FirebaseMessaging.getInstance().deleteToken().addOnFailureListener {
                if (!emitter.isDisposed)
                    emitter.onError(it)
            }
        }
}
