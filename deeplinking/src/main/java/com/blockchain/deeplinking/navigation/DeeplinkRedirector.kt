package com.blockchain.deeplinking.navigation

import android.net.Uri
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2
import com.blockchain.notifications.models.NotificationPayload
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber

class DeeplinkRedirector(private val deeplinkProcessorV2: DeeplinkProcessorV2) {

    private val _deeplinkEvents = PublishSubject.create<DeepLinkResult.DeepLinkResultSuccess>()
    val deeplinkEvents: Observable<DeepLinkResult.DeepLinkResultSuccess>
        get() = _deeplinkEvents

    fun processDeeplinkURL(url: Uri, payload: NotificationPayload? = null) : Completable =
        Completable.fromCallable {
            deeplinkProcessorV2.process(url, payload).subscribeBy(
                onSuccess = { result ->
                    if (result is DeepLinkResult.DeepLinkResultSuccess) {
                        _deeplinkEvents.onNext(result as DeepLinkResult.DeepLinkResultSuccess?)
                    } else {
                        throw Exception("Unable to process deeplink URL")
                    }
                },
                onError = { throw Exception(it) }
            )
        }
}
