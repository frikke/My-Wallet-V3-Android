package piuk.blockchain.android.ui.kyc.settings

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTiers
import io.reactivex.rxjava3.core.Single
import timber.log.Timber

class KycStatusHelper(
    private val kycService: KycService
) {

    // TODO(aromano): check usages and if we can remove this helper altogether
    fun getKycTierStatus(): Single<KycTiers> =
        kycService.getTiersLegacy()
            .onErrorReturn { KycTiers.default() }
            .doOnError { Timber.e(it) }
}
