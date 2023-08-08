package piuk.blockchain.android.ui.kyc.reentry

import androidx.navigation.NavDirections
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.ui.kyc.navhost.toProfileModel

interface ReentryDecision {

    fun findReentryPoint(user: NabuUser): Single<ReentryPoint>
}

interface KycNavigator {

    /**
     * Will fetch user, if you have it, user overload.
     */
    fun findNextStep(): Single<NavDirections>

    fun findNextStep(user: NabuUser): Single<NavDirections>

    fun userAndReentryPointToDirections(user: NabuUser, reentryPoint: ReentryPoint): NavDirections
}

class ReentryDecisionKycNavigator(
    private val userService: UserService,
    private val reentryDecision: ReentryDecision,
    private val analytics: Analytics
) : KycNavigator {

    override fun findNextStep(): Single<NavDirections> =
        userService.getUser()
            .flatMap { findNextStep(it) }

    override fun findNextStep(user: NabuUser): Single<NavDirections> =
        reentryDecision.findReentryPoint(user)
            .map { userAndReentryPointToDirections(user, it) }

    override fun userAndReentryPointToDirections(user: NabuUser, reentryPoint: ReentryPoint): NavDirections =
        when (reentryPoint) {
            ReentryPoint.EmailEntry -> {
                analytics.logEvent(KYCAnalyticsEvents.EmailVeriffRequested(LaunchOrigin.VERIFICATION))
                KycNavXmlDirections.actionStartEmailVerification(
                    /* mustBeValidated = */ true,
                    /* legacyToolbar = */ true
                )
            }

            ReentryPoint.CountrySelection -> KycNavXmlDirections.actionStartCountrySelection()
            ReentryPoint.Profile -> KycNavXmlDirections.actionStartProfile(
                user.requireCountryCode(),
                user.address?.stateIso ?: "",
                user.address?.stateIso ?: ""
            )

            ReentryPoint.Address -> {
                KycNavXmlDirections.actionStartAutocompleteAddressEntry(user.toProfileModel())
            }

            is ReentryPoint.Questionnaire ->
                KycNavXmlDirections.actionStartQuestionnaireEntry(reentryPoint.questionnaire, user.requireCountryCode())

            ReentryPoint.MobileEntry -> KycNavXmlDirections.actionStartMobileVerification(user.requireCountryCode())
            ReentryPoint.Veriff -> {
                val countryCode = user.requireCountryCode()
                KycNavXmlDirections.actionStartVeriff(countryCode)
            }

            is ReentryPoint.TierCurrentState -> KycNavXmlDirections.actionStartTierCurrentState(reentryPoint.kycState)
        }
}
