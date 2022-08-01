package com.blockchain.analytics.data

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.AnalyticsLocalPersistence
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.NabuAnalyticsEvent
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.operations.AppStartUpFlushable
import com.blockchain.utils.Optional
import com.blockchain.utils.toUtcIso8601
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import piuk.blockchain.androidcore.utils.SessionPrefs
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class NabuAnalytics(
    private val analyticsService: AnalyticsService,
    private val prefs: Lazy<SessionPrefs>,
    private val localAnalyticsPersistence: AnalyticsLocalPersistence,
    private val remoteLogger: RemoteLogger,
    lifecycleObservable: LifecycleObservable,
    private val analyticsContextProvider: AnalyticsContextProvider,
    private val tokenStore: NabuSessionTokenStore
) : Analytics, AppStartUpFlushable {
    private val compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += lifecycleObservable.onStateUpdated.filter { it == AppState.BACKGROUNDED }
            .flatMapCompletable {
                flush().onErrorComplete()
            }.emptySubscribe()
    }

    private val id: String by lazy {
        prefs.value.deviceId
    }

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        val nabuEvent = analyticsEvent.toNabuAnalyticsEvent()
        logEventInTerminal(analyticsEvent)

        compositeDisposable += localAnalyticsPersistence.save(nabuEvent)
            .subscribeOn(Schedulers.computation())
            .doOnError {
                remoteLogger.logException(it)
            }
            .onErrorComplete()
            .then {
                sendToApiAndFlushIfNeeded()
            }
            .emptySubscribe()
    }

    private fun logEventInTerminal(analyticsEvent: AnalyticsEvent) {
        var paramsString = ""
        for ((key, value) in analyticsEvent.params) {
            paramsString += " $key = $value\n "
        }
        Timber.i(
            " \nevent: %s \n origin: %s \n params: \n %s",
            analyticsEvent.event,
            analyticsEvent.origin ?: "",
            paramsString
        )
    }

    private fun sendToApiAndFlushIfNeeded(): Completable {
        return localAnalyticsPersistence.size().flatMapCompletable {
            if (it >= BATCH_SIZE) {
                batchToApiAndFlush()
                    .doOnError {
                        remoteLogger.logException(it, "Error sending batched analytics")
                    }
                    .onErrorComplete()
            } else {
                Completable.complete()
            }
        }
    }

    override val tag: String
        get() = "nabu_analytics_flush"

    override fun flush(): Completable {
        return localAnalyticsPersistence.getAllItems().flatMapCompletable { events ->
            // Whats happening here is that we split the retrieved items into sublists of size = BATCH_SIZE
            // and then each one of these sublists is converted to the corresponding completable that actually is the
            // api request.
            val listOfSublists = mutableListOf<List<NabuAnalyticsEvent>>()
            for (i in events.indices step BATCH_SIZE) {
                listOfSublists.add(
                    events.subList(i, (i + BATCH_SIZE).coerceAtMost(events.size))
                )
            }

            val completables = listOfSublists.map { list ->
                postEvents(list).then {
                    localAnalyticsPersistence.removeOldestItems(list.size)
                }
            }
            Completable.concat(completables)
        }
    }

    private fun batchToApiAndFlush(): Completable {
        return localAnalyticsPersistence.getOldestItems(BATCH_SIZE).flatMapCompletable {
            postEvents(it)
        }.then {
            localAnalyticsPersistence.removeOldestItems(BATCH_SIZE)
        }
    }

    private fun postEvents(events: List<NabuAnalyticsEvent>): Completable =
        tokenStore.getAccessToken().firstOrError().flatMapCompletable {
            analyticsService.postEvents(
                events = events,
                id = id,
                analyticsContext = analyticsContextProvider.context(),
                platform = "WALLET",
                device = "APP-Android",
                authorization = if (it is Optional.Some) it.element.authHeader else null
            )
        }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {}

    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {}

    companion object {
        private const val BATCH_SIZE = 30
    }
}

private fun AnalyticsEvent.toNabuAnalyticsEvent(): NabuAnalyticsEvent =
    NabuAnalyticsEvent(
        name = this.event,
        type = "EVENT",
        originalTimestamp = Date().toUtcIso8601(Locale.ENGLISH),
        properties = this.params.filterValues { it is String }.mapValues { it.value.toString() }
            .plusOriginIfAvailable(this.origin),
        numericProperties = this.params.filterValues { it is Number }.mapValues { BigDecimal(it.value.toString()) },
        booleanProperties = this.params.filterValues { it is Boolean }.mapValues { it.value as Boolean }
    )

private fun Map<String, String>.plusOriginIfAvailable(launchOrigin: LaunchOrigin?): Map<String, String> {
    val origin = launchOrigin ?: return this
    return this.toMutableMap().apply {
        this["origin"] = origin.name
    }.toMap()
}
