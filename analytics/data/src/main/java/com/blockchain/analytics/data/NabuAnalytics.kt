package com.blockchain.analytics.data

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsContext
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.AnalyticsLocalPersistence
import com.blockchain.analytics.AnalyticsSettings
import com.blockchain.analytics.NabuAnalyticsEvent
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.api.services.AnalyticsService
import com.blockchain.koin.payloadScope
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.preferences.SessionPrefs
import com.blockchain.utils.Optional
import com.blockchain.utils.emptySubscribe
import com.blockchain.utils.then
import com.blockchain.utils.toJsonElement
import com.blockchain.utils.toUtcIso8601
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber

class NabuAnalytics(
    private val analyticsService: AnalyticsService,
    private val prefs: Lazy<SessionPrefs>,
    private val localAnalyticsPersistence: AnalyticsLocalPersistence,
    private val remoteLogger: RemoteLogger,
    lifecycleObservable: LifecycleObservable,
    private val analyticsContextProvider: AnalyticsContextProvider,
    private val tokenStore: NabuSessionTokenStore
) : Analytics, AnalyticsSettings {
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
        logEventInTerminal(analyticsEvent)

        compositeDisposable += payloadScope.get<WalletModeService>().walletModeSingle
            .flatMapCompletable { walletMode ->
                localAnalyticsPersistence.save(
                    analyticsEvent.toNabuAnalyticsEvent().withWalletMode(walletMode)
                )
            }
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
                postEvents(events = list).then {
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
        rxSingle { analyticsContextProvider.context() }
            .plusWalletModeTrait(events = events)
            .flatMapCompletable { context ->
                tokenStore.getAccessToken().firstOrError().flatMapCompletable {
                    analyticsService.postEvents(
                        events = events,
                        id = id,
                        analyticsContext = context,
                        platform = "WALLET",
                        device = "APP-Android",
                        authorization = if (it is Optional.Some) it.element.authHeader else null
                    )
                }
            }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {}

    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {}

    companion object {
        private const val BATCH_SIZE = 30
    }
}

private fun Single<AnalyticsContext>.plusWalletModeTrait(
    events: List<NabuAnalyticsEvent>
): Single<AnalyticsContext> {
    val appModeTrait = (
        events.firstOrNull { it.properties.containsKey("app_mode") }
            ?.properties?.get("app_mode") as? JsonPrimitive?
        )?.content

    fun AnalyticsContext.withWalletModeTrait(appModeTrait: String): AnalyticsContext {
        return copy(
            traits = traits + mapOf(
                "app_mode" to appModeTrait
            )
        )
    }

    return flatMap { analyticsContext ->
        appModeTrait?.let {
            Single.just(
                analyticsContext.withWalletModeTrait(appModeTrait)
            )
        } ?: kotlin.run {
            // default to current app mode
            val walletModeService = payloadScope.get<WalletModeService>()
            rxSingle { walletModeService.walletModeSingle.await() }
                .map { appMode ->
                    analyticsContext.withWalletModeTrait(appMode.toTraitsString())
                }
        }
    }
}

private fun AnalyticsEvent.toNabuAnalyticsEvent(): NabuAnalyticsEvent =
    NabuAnalyticsEvent(
        name = this.event,
        type = "EVENT",
        originalTimestamp = Date().toUtcIso8601(Locale.ENGLISH),
        properties = this.params.mapValues {
            it.value.toJsonElement()
        }.plusOriginIfAvailable(this.origin)
    )

private fun NabuAnalyticsEvent.withWalletMode(walletMode: WalletMode) = copy(
    properties = properties.plus("app_mode" to walletMode.toTraitsString().toJsonElement())
)

private fun Map<String, JsonElement>.plusOriginIfAvailable(launchOrigin: LaunchOrigin?): Map<String, JsonElement> {
    val origin = launchOrigin ?: return this
    return this.toMutableMap().apply {
        this["origin"] = JsonPrimitive(origin.name)
    }.toMap()
}

private fun WalletMode.toTraitsString(): String {
    return when (this) {
        WalletMode.CUSTODIAL -> "TRADING"
        WalletMode.NON_CUSTODIAL -> "PKW"
    }
}
