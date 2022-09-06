package piuk.blockchain.android.ui.kyc.navhost

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import piuk.blockchain.android.ui.kyc.address.models.OldProfileModel
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import timber.log.Timber

class KycNavHostPresenter(
    nabuToken: NabuToken,
    private val userService: UserService,
    private val reentryDecision: ReentryDecision,
    private val kycNavigator: KycNavigator,
    private val kycTiersStore: KycTiersStore,
    private val productEligibilityStore: ProductsEligibilityStore,
    private val getUserStore: GetUserStore,
    private val analytics: Analytics,
) : BaseKycPresenter<KycNavHostView>(nabuToken) {

    override fun onViewReady() {
        kycTiersStore.invalidate()
        getUserStore.invalidate()
        productEligibilityStore.invalidate()

        compositeDisposable +=
            userService.getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.displayLoading(true) }
                .subscribeBy(
                    onSuccess = {
                        registerForCampaignsIfNeeded()
                        redirectUserFlow(it)
                    },
                    onError = {
                        Timber.e(it)
                        if (it is MetadataNotFoundException) {
                            // No user, hide loading and start full KYC flow
                            view.displayLoading(false)
                        } else {
                            view.showErrorSnackbarAndFinish(R.string.kyc_status_error)
                        }
                    }
                )
    }

    /**
     * Registers the user to the various campaigns if they are not yet registered with them, on completion of Gold
     */
    private fun registerForCampaignsIfNeeded() {
    }

    private fun redirectUserFlow(user: NabuUser) {
        when {
            view.campaignType == CampaignType.Resubmission || user.isMarkedForResubmission -> {
                view.navigateToResubmissionSplash()
            }
            view.campaignType == CampaignType.SimpleBuy ||
                view.campaignType == CampaignType.Interest ||
                view.campaignType == CampaignType.FiatFunds ||
                view.campaignType == CampaignType.None ||
                view.campaignType == CampaignType.Swap -> {
                compositeDisposable += kycNavigator.findNextStep()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { Timber.e(it) },
                        onSuccess = { view.navigate(it) }
                    )
            }
            user.state != UserState.None && user.kycState == KycState.None -> {
                val current = user.tiers?.current
                if (current == null || current == 0) {
                    compositeDisposable += reentryDecision.findReentryPoint(user)
                        .subscribeBy(
                            onSuccess = { reentryPoint ->
                                val directions = kycNavigator.userAndReentryPointToDirections(user, reentryPoint)
                                view.navigate(directions)
                                analytics.logEvent(KYCAnalyticsEvents.KycResumedEvent(reentryPoint.entryPoint))
                            },
                            onError = {
                                Timber.e(it)
                                view.showErrorSnackbarAndFinish(R.string.kyc_status_error)
                            }
                        )
                }
            }
        }

        // If no other methods are triggered, this will start KYC from scratch. If others have been called,
        // this will make the host fragment visible.
        view.displayLoading(false)
    }
}

internal fun NabuUser.toProfileModel(): ProfileModel = ProfileModel(
    firstName = firstName ?: throw IllegalStateException("First Name is null"),
    lastName = lastName ?: throw IllegalStateException("Last Name is null"),
    countryCode = address?.countryCode ?: throw IllegalStateException("Country Code is null"),
    stateCode = address?.stateIso
)

internal fun NabuUser.toOldProfileModel(): OldProfileModel = OldProfileModel(
    firstName = firstName ?: throw IllegalStateException("First Name is null"),
    lastName = lastName ?: throw IllegalStateException("Last Name is null"),
    countryCode = address?.countryCode ?: throw IllegalStateException("Country Code is null"),
    stateCode = address?.stateIso,
    stateName = address?.stateIso
)
