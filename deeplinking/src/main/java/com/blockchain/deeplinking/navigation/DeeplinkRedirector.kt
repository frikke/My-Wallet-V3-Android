package com.blockchain.deeplinking.navigation

import android.net.Uri
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2
import com.blockchain.notifications.models.NotificationPayload
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class DeeplinkRedirector(private val deeplinkProcessorV2: DeeplinkProcessorV2) {

    private val _deeplinkEvents = PublishSubject.create<DeepLinkResult.DeepLinkResultSuccess>()
    val deeplinkEvents: Observable<DeepLinkResult.DeepLinkResultSuccess>
        get() = _deeplinkEvents

    fun processDeeplinkURL(url: Uri, payload: NotificationPayload? = null): Completable =
        deeplinkProcessorV2.process(url, payload).flatMapCompletable { result ->
            when (result) {
                is DeepLinkResult.DeepLinkResultSuccess -> {
                    _deeplinkEvents.onNext(result as DeepLinkResult.DeepLinkResultSuccess?)
                    Completable.complete()
                }
                is DeepLinkResult.DeepLinkResultFailed -> {
                    throw Exception("Unable to process deeplink URL. ${result.uri?.path}")
                }
            }
        }
}
