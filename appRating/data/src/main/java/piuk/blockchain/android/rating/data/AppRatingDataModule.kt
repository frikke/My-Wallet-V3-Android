package piuk.blockchain.android.rating.data

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.data.repository.AppRatingRepository
import piuk.blockchain.android.rating.domain.service.AppRatingService

val appRatingDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            AppRatingRemoteConfig(
                remoteConfig = get()
            )
        }

        scoped<AppRatingService> {
            AppRatingRepository(
                appRatingRemoteConfig = get()
            )
        }
    }
}
