package piuk.blockchain.com

import com.blockchain.koin.featureFlagsPrefs
import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.walletmode.WalletModeService
import org.koin.dsl.bind
import org.koin.dsl.module

val internalFeatureFlagsModule = module {
    single {
        FeatureFlagOverridePrefsDebugImpl(
            store = get(featureFlagsPrefs)
        )
    }.bind(FeatureFlagOverridePrefs::class)

    single {
        WalletModeRepository(
            sharedPreferences = get()
        )
    }.bind(WalletModeService::class)
}
