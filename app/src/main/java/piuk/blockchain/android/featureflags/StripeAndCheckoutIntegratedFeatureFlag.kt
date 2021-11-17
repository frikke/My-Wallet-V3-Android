package piuk.blockchain.android.featureflags

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class StripeAndCheckoutIntegratedFeatureFlag(
    private val gatedFeatures: InternalFeatureFlagApi,
    private val remoteFlag: FeatureFlag
) : FeatureFlag {
    override val enabled: Single<Boolean> by unsafeLazy {
        remoteFlag.enabled
            .cache()
            .map { remoteEnable ->
                remoteEnable && gatedFeatures.isFeatureEnabled(GatedFeature.STRIPE_CHECKOUT_PAYMENTS)
            }
    }
}
