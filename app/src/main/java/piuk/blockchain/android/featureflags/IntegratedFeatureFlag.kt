package piuk.blockchain.android.featureflags

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

abstract class IntegratedFeatureFlag : FeatureFlag, KoinComponent {

    private val environmentConfig: EnvironmentConfig by inject()

    protected abstract fun isLocalEnabled(): Boolean
    protected abstract fun isRemoteEnabled(): Single<Boolean>

    override val enabled: Single<Boolean>
        get() = if (environmentConfig.isRunningInDebugMode()) {
            Single.just(isLocalEnabled())
        } else {
            if (isLocalEnabled()) Single.just(true)
            else isRemoteEnabled()
        }
}
