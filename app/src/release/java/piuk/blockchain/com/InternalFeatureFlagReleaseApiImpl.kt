package piuk.blockchain.com

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi

class InternalFeatureFlagReleaseApiImpl(
    private val environmentConfig: EnvironmentConfig
) : InternalFeatureFlagApi {
    override fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean = environmentConfig.isCompanyInternalBuild() &&
        gatedFeature.enabledForCompanyInternalBuild

    override fun enable(gatedFeature: GatedFeature) {
        // do nothing
    }

    override fun disable(gatedFeature: GatedFeature) {
        // do nothing
    }

    override fun disableAll() {
        // do nothing
    }

    override fun getAll(): Map<GatedFeature, Boolean> {
        // do nothing
        return mapOf()
    }
}
