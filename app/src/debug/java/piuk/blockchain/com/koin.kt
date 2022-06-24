package piuk.blockchain.com

import com.blockchain.koin.featureFlagsPrefs
import com.blockchain.preferences.FeatureFlagOverridePrefs
import org.koin.dsl.bind
import org.koin.dsl.module

val internalFeatureFlagsModule = module {
    single {
        FeatureFlagOverridePrefsDebugImpl(
            store = get(featureFlagsPrefs)
        )
    }.bind(FeatureFlagOverridePrefs::class)
}
