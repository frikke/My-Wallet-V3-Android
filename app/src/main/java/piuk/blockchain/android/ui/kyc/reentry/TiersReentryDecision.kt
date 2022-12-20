package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.utils.rxMaybeOutcome
import io.reactivex.rxjava3.core.Single

class TiersReentryDecision(
    private val custodialWalletManager: CustodialWalletManager,
    private val dataRemediationService: DataRemediationService,
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
        val entryPoint = when {
            tier0UnverifiedEmail() -> ReentryPoint.EmailEntry
            tier0UnselectedCountry() -> ReentryPoint.CountrySelection
            tier0ProfileIncompleteOrResubmitAllowed() &&
                !tier0UnselectedCountry() -> ReentryPoint.Profile
            tier0AndCanAdvance() && tier0MissingAddress() -> ReentryPoint.Address
            !hasMobileVerified() -> ReentryPoint.MobileEntry
            else -> return rxMaybeOutcome {
                dataRemediationService.getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION)
            }.map { questionnaire ->
                ReentryPoint.Questionnaire(questionnaire) as ReentryPoint
            }.defaultIfEmpty(ReentryPoint.Veriff)
        }

        return Single.just(entryPoint)
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
