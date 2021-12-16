package piuk.blockchain.android.featureflags

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import io.reactivex.rxjava3.core.Single

class DashboardOnboardingIntegratedFeatureFlag(
    private val gatedFeatures: InternalFeatureFlagApi,
    private val remoteFlag: FeatureFlag
) : IntegratedFeatureFlag() {
    override fun isLocalEnabled(): Boolean = gatedFeatures.isFeatureEnabled(GatedFeature.DASHBOARD_ONBOARDING)
    override fun isRemoteEnabled(): Single<Boolean> = remoteFlag.enabled.cache()
}
