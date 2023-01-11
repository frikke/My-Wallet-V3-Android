package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.utils.rxMaybeOutcome
import com.blockchain.utils.rxSingleOutcome
import io.reactivex.rxjava3.core.Single

class TiersReentryDecision(
    private val custodialWalletManager: CustodialWalletManager,
    private val dataRemediationService: DataRemediationService,
    private val kycService: KycService,
) : ReentryDecision {

    private lateinit var nabuUser: NabuUser
    private val isTierZero: Boolean by lazy {
        nabuUser.tiers?.current == 0
    }

    override fun findReentryPoint(user: NabuUser): Single<ReentryPoint> {
        if (user.kycState != KycState.None) {
            return custodialWalletManager.fetchSimplifiedDueDiligenceUserState().map {
                ReentryPoint.TierCurrentState(
                    kycState = user.kycState,
                    isSddVerified = it.isVerified,
                )
            }
        }

        nabuUser = user
        return when {
            tier0UnverifiedEmail() -> Single.just(ReentryPoint.EmailEntry)
            tier0UnselectedCountry() -> Single.just(ReentryPoint.CountrySelection)
            else -> rxSingleOutcome { kycService.shouldLaunchProve() }
                .onErrorReturnItem(false)
                .flatMap { shouldLaunchProve ->
                    if (shouldLaunchProve) {
                        Single.just(ReentryPoint.Prove)
                    } else {
                        when {
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
