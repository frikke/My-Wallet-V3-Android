package com.blockchain.deeplinking.navigation

import android.net.Uri
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2
import com.blockchain.notifications.models.NotificationPayload
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject

class DeeplinkRedirector(private val deeplinkProcessorV2: DeeplinkProcessorV2) {

    private val _deeplinkEvents = PublishSubject.create<DeepLinkResult>()
    val deeplinkEvents: Observable<DeepLinkResult>
        get() = _deeplinkEvents

    fun processDeeplinkURL(url: Uri, payload: NotificationPayload? = null): Single<DeepLinkResult> =
        deeplinkProcessorV2.process(url, payload).map { result ->
            // fire a new event to the global handler
            _deeplinkEvents.onNext(result)
            // and return the single value for individual subscribers of this method
            return@map result
        }
}
