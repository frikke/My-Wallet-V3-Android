package com.blockchain.coreandroid

import android.content.Context
import android.preference.PreferenceManager
import com.blockchain.core.utils.DeviceIdGeneratorService
import com.blockchain.core.utils.EncryptedPrefs
import com.blockchain.coreandroid.remoteconfig.RemoteConfigRepository
import com.blockchain.coreandroid.utils.AndroidDeviceIdGeneratorRepository
import com.blockchain.coreandroid.utils.CloudBackupAgent
import com.blockchain.coreandroid.utils.PrefsUtil
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.koin.featureFlagsPrefs
import com.blockchain.preferences.AppInfoPrefs
import com.blockchain.preferences.AppMaintenancePrefs
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.CountryPrefs
import com.blockchain.preferences.CowboysPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.preferences.DexPrefs
import com.blockchain.preferences.ExchangeCampaignPrefs
import com.blockchain.preferences.HandholdPrefs
import com.blockchain.preferences.IterableAnnouncementsPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.preferences.MaskedValuePrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.preferences.NotificationPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.PricesPrefs
import com.blockchain.preferences.RecurringBuyPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.preferences.RuntimePermissionsPrefs
import com.blockchain.preferences.SecureChannelPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.preferences.SessionPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.preferences.SmallBalancesPrefs
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.preferences.ThemePrefs
import com.blockchain.preferences.TransactionPrefs
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import org.koin.dsl.bind
import org.koin.dsl.module

val coreAndroidModule = module {

    factory {
        AndroidDeviceIdGeneratorRepository(
            platformDeviceIdGenerator = get(),
            analytics = get()
        )
    }.bind(DeviceIdGeneratorService::class)

    single {
        PrefsUtil(
            ctx = get(),
            store = get(),
            backupStore = CloudBackupAgent.backupPrefs(ctx = get()),
            idGenerator = get(),
            uuidGenerator = get(),
            assetCatalogue = get(),
            environmentConfig = get()
        )
    }.apply {
        bind(SessionPrefs::class)
        bind(CurrencyPrefs::class)
        bind(SmallBalancesPrefs::class)
        bind(NotificationPrefs::class)
        bind(DashboardPrefs::class)
        bind(DexPrefs::class)
        bind(SecurityPrefs::class)
        bind(CountryPrefs::class)
        bind(PricesPrefs::class)
        bind(WalletModePrefs::class)
        bind(RemoteConfigPrefs::class)
        bind(SimpleBuyPrefs::class)
        bind(RecurringBuyPrefs::class)
        bind(WalletStatusPrefs::class)
        bind(EncryptedPrefs::class)
        bind(TransactionPrefs::class)
        bind(AuthPrefs::class)
        bind(AppInfoPrefs::class)
        bind(BankLinkingPrefs::class)
        bind(SecureChannelPrefs::class)
        bind(OnboardingPrefs::class)
        bind(AppMaintenancePrefs::class)
        bind(AppRatingPrefs::class)
        bind(NftAnnouncementPrefs::class)
        bind(ReferralPrefs::class)
        bind(LocalSettingsPrefs::class)
        bind(SuperAppMvpPrefs::class)
        bind(CowboysPrefs::class)
        bind(ExchangeCampaignPrefs::class)
        bind(IterableAnnouncementsPrefs::class)
        bind(MaskedValuePrefs::class)
        bind(HandholdPrefs::class)
        bind(ThemePrefs::class)
        bind(RuntimePermissionsPrefs::class)
    }

    factory {
        PreferenceManager.getDefaultSharedPreferences(
            /* context = */
            get()
        )
    }

    factory(featureFlagsPrefs) {
        get<Context>().getSharedPreferences("FeatureFlagsPrefs", Context.MODE_PRIVATE)
    }

    single {
        RemoteConfigRepository(
            firebaseRemoteConfig = get(),
            remoteConfigPrefs = get(),
            experimentsStore = get(),
            json = get()
        )
    }.bind(RemoteConfigService::class)
}
