package piuk.blockchain.android.ui.home.v2

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import io.reactivex.rxjava3.core.Single

class FabSheetBuySellOrderingFeatureFlag(
    private val localApi: InternalFeatureFlagApi,
    private val remoteConfig: FeatureFlag
) : IntegratedFeatureFlag() {

    override fun isLocalEnabled(): Boolean = localApi.isFeatureEnabled(GatedFeature.FAB_SHEET_CTAS)

    override fun isRemoteEnabled(): Single<Boolean> = remoteConfig.enabled
}
