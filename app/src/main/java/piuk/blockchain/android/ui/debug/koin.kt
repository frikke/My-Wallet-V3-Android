package piuk.blockchain.android.ui.debug

import com.blockchain.koin.modules.getFeatureFlags
import org.koin.dsl.module

val remoteFeatureFlagsModule = module {
    single {
        FeatureFlagHandler(
            featureFlags = getFeatureFlags(),
            prefs = get()
        )
    }
}
