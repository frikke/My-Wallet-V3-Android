package piuk.blockchain.android.featureflags

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class WalletRedesignIntegratedFeatureFlag(
    private val gatedFeatures: InternalFeatureFlagApi,
    private val remoteFlag: FeatureFlag
) : FeatureFlag {
    override val enabled: Single<Boolean> by unsafeLazy {
        if (gatedFeatures.isFeatureEnabled(GatedFeature.WALLET_REDESIGN)) {
            Single.just(true)
        } else {
            remoteFlag.enabled.cache()
        }
    }
}
