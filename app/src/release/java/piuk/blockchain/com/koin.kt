package piuk.blockchain.com

import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.walletmode.WalletModeService
import org.koin.dsl.bind
import org.koin.dsl.module

val internalFeatureFlagsModule = module {
    single {
        FeatureFlagOverridePrefsReleaseImpl()
    }.bind(FeatureFlagOverridePrefs::class)

    single {
        WalletModeRepository()
    }.bind(WalletModeService::class)
}
