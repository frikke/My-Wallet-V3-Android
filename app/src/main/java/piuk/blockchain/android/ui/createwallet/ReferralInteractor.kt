package piuk.blockchain.android.ui.createwallet

import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralValidity
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class ReferralInteractor(
    private val referralService: ReferralService
) {

    fun validateReferralIfNeeded(referralCode: String?): Single<ReferralCodeState> {
        return if (referralCode.isNullOrEmpty()) {
            Single.just(ReferralCodeState.VALID)
        } else {
            rxSingleOutcome {
                referralService.validateReferralCode(referralCode)
            }
                .map { validity ->
                    when (validity) {
                        ReferralValidity.VALID -> ReferralCodeState.VALID
                        ReferralValidity.INVALID -> ReferralCodeState.INVALID
                        else -> ReferralCodeState.NOT_AVAILABLE
                    }
                }
        }
            .applySchedulers()
    }
}

enum class ReferralCodeState {
    NOT_AVAILABLE, INVALID, VALID
}
