package com.blockchain.analytics.data.koin

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.analytics.AnalyticsLocalPersistence
import com.blockchain.analytics.AnalyticsSettings
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.analytics.UserAnalytics
import com.blockchain.analytics.data.AnalyticsContextProviderImpl
import com.blockchain.analytics.data.AnalyticsFileLocalPersistence
import com.blockchain.analytics.data.AnalyticsImpl
import com.blockchain.analytics.data.NabuAnalytics
import com.blockchain.analytics.data.UserAnalyticsImpl
import com.blockchain.koin.nabu
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {

    single { FirebaseAnalytics.getInstance(get()) }

    factory {
        AnalyticsImpl(
            firebaseAnalytics = get(),
            nabuAnalytics = get(nabu),
            nabuAnalyticsSettings = get(nabu),
            remoteLogger = get(),
            store = get()
        )
    }.apply {
        bind(Analytics::class)
        bind(AnalyticsSettings::class)
        bind(ProviderSpecificAnalytics::class)
    }

    factory { UserAnalyticsImpl(get()) }
        .bind(UserAnalytics::class)

    single(nabu) {
        NabuAnalytics(
            analyticsService = get(),
            prefs = lazy { get() },
            analyticsContextProvider = get(),
            localAnalyticsPersistence = get(),
            remoteLogger = get(),
            tokenStore = get(),
            lifecycleObservable = get()
        )
    }.apply {
        bind(AnalyticsSettings::class)
        bind(Analytics::class)
    }

    factory {
        AnalyticsContextProviderImpl(
            traitsServices = getAll()
        )
    }.bind(AnalyticsContextProvider::class)

    single {
        AnalyticsFileLocalPersistence(
            context = get()
        )
    }.bind(AnalyticsLocalPersistence::class)
}
