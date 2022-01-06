package piuk.blockchain.com

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.preferences.FeatureFlagOverridePrefs
import org.koin.dsl.bind
import org.koin.dsl.module

val internalFeatureFlagsModule = module {
    single {
        InternalFeatureFlagReleaseApiImpl(
            environmentConfig = get()
        )
    }.bind(InternalFeatureFlagApi::class)

    single {
        FeatureFlagOverridePrefsReleaseImpl()
    }.bind(FeatureFlagOverridePrefs::class)
}
