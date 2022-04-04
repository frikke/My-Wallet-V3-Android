package com.blockchain.notifications.koin

import android.app.NotificationManager
import android.content.Context
import com.blockchain.koin.nabu
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.notifications.FirebaseNotificationTokenProvider
import com.blockchain.notifications.NotificationService
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.NotificationTokenProvider
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsImpl
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import com.blockchain.notifications.analytics.UserAnalytics
import com.blockchain.notifications.analytics.UserAnalyticsImpl
import com.blockchain.notifications.links.DynamicLinkHandler
import com.blockchain.notifications.links.PendingLink
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.RemoteConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {

    scope(payloadScopeQualifier) {
        scoped {
            NotificationTokenManager(
                notificationService = get(),
                payloadManager = get(),
                prefs = get(),
                remoteLogger = get(),
                authPrefs = get(),
                notificationTokenProvider = get()
            )
        }

        factory {
            FirebaseNotificationTokenProvider()
        }.bind(NotificationTokenProvider::class)
    }
    single { FirebaseAnalytics.getInstance(get()) }

    factory { NotificationService(get()) }

    factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) }.bind(NotificationManager::class)

    single { FirebaseDynamicLinks.getInstance() }

    factory { DynamicLinkHandler(get()) }.bind(PendingLink::class)

    factory {
        AnalyticsImpl(
            firebaseAnalytics = get(),
            nabuAnalytics = get(nabu),
            store = get()
        )
    }
        .bind(Analytics::class)
        .bind(ProviderSpecificAnalytics::class)

    factory { UserAnalyticsImpl(get()) }
        .bind(UserAnalytics::class)

    single {
        val config = FirebaseRemoteConfigSettings.Builder()
            .build()
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(config)
        }
    }

    factory {
        RemoteConfiguration(
            remoteConfig = get(),
            environmentConfig = get(),
            remoteConfigPrefs = get()
        )
    }.bind(RemoteConfig::class)
        .bind(ABTestExperiment::class)
}
