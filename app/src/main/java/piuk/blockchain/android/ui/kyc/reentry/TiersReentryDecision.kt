package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.data.asSingle
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.utils.rxMaybeOutcome
import io.reactivex.rxjava3.core.Single

class TiersReentryDecision(
    private val dataRemediationService: DataRemediationService,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : ReentryDecision {

    private lateinit var nabuUser: NabuUser
    private val isTierZero: Boolean by lazy {
        nabuUser.tiers?.current == 0
    }

    override fun findReentryPoint(user: NabuUser): Single<ReentryPoint> =
        userFeaturePermissionService.isEligibleFor(Feature.Kyc).asSingle().flatMap { isEligibleToKyc ->
            if (user.kycState != KycState.None) {
                return@flatMap Single.just(ReentryPoint.TierCurrentState(user.kycState))
            } else if (!isEligibleToKyc) {
                return@flatMap Single.just(ReentryPoint.TierCurrentState(KycState.Rejected))
            }

            nabuUser = user
            return@flatMap when {
                tier0UnverifiedEmail() -> Single.just(ReentryPoint.EmailEntry)
                tier0UnselectedCountry() -> Single.just(ReentryPoint.CountrySelection)
                else -> when {
                    tier0ProfileIncompleteOrResubmitAllowed() &&
                        !tier0UnselectedCountry() -> Single.just(ReentryPoint.Profile)
                    tier0AndCanAdvance() && tier0MissingAddress() -> Single.just(ReentryPoint.Address)
                    !hasMobileVerified() -> Single.just(ReentryPoint.MobileEntry)
                    else -> rxMaybeOutcome {
                        dataRemediationService.getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION)
                    }.map { questionnaire ->
                        ReentryPoint.Questionnaire(questionnaire) as ReentryPoint
                    }.defaultIfEmpty(ReentryPoint.Veriff)
                }
            }
        }

    private fun tier0UnverifiedEmail(): Boolean = isTierZero && !nabuUser.emailVerified

    private fun tier0UnselectedCountry(): Boolean = isTierZero && nabuUser.address?.countryCode.isNullOrBlank()

    private fun tier0ProfileIncompleteOrResubmitAllowed(): Boolean {
        return isTierZero &&
            (
                nabuUser.isProfileIncomplete() ||
                    nabuUser.isMarkedForRecoveryResubmission
                )
    }

    private fun tier0AndCanAdvance() = isTierZero && nabuUser.tiers!!.next == 1

    private fun tier0MissingAddress() =
        isTierZero &&
            nabuUser.address?.line1.isNullOrEmpty() &&
            nabuUser.address?.line2.isNullOrEmpty() &&
            nabuUser.address?.city.isNullOrEmpty() &&
            nabuUser.address?.postCode.isNullOrEmpty()

    private fun hasMobileVerified() = nabuUser.mobileVerified
}

private fun NabuUser.isProfileIncomplete() =
    firstName.isNullOrBlank() ||
        lastName.isNullOrBlank() ||
        dob.isNullOrBlank()
