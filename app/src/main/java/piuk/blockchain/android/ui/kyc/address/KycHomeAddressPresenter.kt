package piuk.blockchain.android.ui.kyc.address

import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.network.PollService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.SortedMap
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

interface KycNextStepDecision {

    sealed class NextStep(val order: Int) : Comparable<NextStep> {
        object Tier1Complete : NextStep(0)
        object SDDComplete : NextStep(1)
        object Tier2ContinueTier1NeedsMoreInfo : NextStep(2)
        data class Questionnaire(val questionnaire: com.blockchain.domain.dataremediation.model.Questionnaire) :
            NextStep(3)
        object Veriff : NextStep(4)

        override fun compareTo(other: NextStep): Int = this.order - other.order
    }

    fun nextStep(): Single<NextStep>
}

class KycHomeAddressPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val eligibilityService: EligibilityService,
    private val nabuDataUserProvider: NabuDataUserProvider,
    private val nabuUserSync: NabuUserSync,
    private val kycNextStepDecision: KycHomeAddressNextStepDecision,
    private val custodialWalletManager: CustodialWalletManager,
    private val analytics: Analytics,
) : BaseKycPresenter<KycHomeAddressView>(nabuToken) {

    val countryCodeSingle: Single<SortedMap<String, String>> by unsafeLazy {
        fetchOfflineToken
            .flatMap {
                rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
                    eligibilityService.getCountriesList(GetRegionScope.None)
                }.subscribeOn(Schedulers.io())
            }
            .map { list ->
                list.associateBy({ it.name }, { it.countryCode })
                    .toSortedMap()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .cache()
    }

    override fun onViewReady() {
        compositeDisposable += view.address
            .subscribeBy(
                onNext = { enableButtonIfComplete(it) },
                onError = {
                    Timber.e(it)
                    // This is fatal - back out and allow the user to try again
                    view.finishPage()
                }
            )

        restoreDataIfPresent()
    }

    private fun restoreDataIfPresent() {
        compositeDisposable +=
            view.address
                .firstElement()
                .flatMap { addressModel ->
                    // Don't attempt to restore state if data is already present
                    if (addressModel.containsData()) {
                        Maybe.empty()
                    } else {
                        nabuDataUserProvider.getUser()
                            .subscribeOn(Schedulers.io())
                            .flatMapMaybe { user ->
                                user.address?.let { address ->
                                    Maybe.just(address)
                                        .flatMap { getCountryName(address.countryCode!!) }
                                        .map { it to address }
                                } ?: Maybe.empty()
                            }
                            .observeOn(AndroidSchedulers.mainThread())
                    }
                }
                .subscribeBy(
                    onSuccess = { (countryName, address) ->
                        view.restoreUiState(
                            address.line1,
                            address.line2,
                            address.city,
                            address.state,
                            address.postCode,
                            countryName
                        )
                    },
                    onError = {
                        // Silently fail
                        Timber.e(it)
                    }
                )
    }

    private data class State(
        val progressToKycNextStep: KycNextStepDecision.NextStep,
        val countryCode: String,
    )

    internal fun onContinueClicked(campaignType: CampaignType? = null) {
        compositeDisposable += view.address
            .firstOrError()
            .flatMap { address ->
                addAddress(address).toSingle { address.country }
            }
            .flatMap { countryCode ->
                updateNabuData().thenSingle { Single.just(countryCode) }
            }
            .map { countryCode ->
                State(
                    progressToKycNextStep = KycNextStepDecision.NextStep.Tier1Complete,
                    countryCode = countryCode
                )
            }
            .zipWith(kycNextStepDecision.nextStep())
            .map { (x, progress) -> x.copy(progressToKycNextStep = progress) }
            .flatMap { state ->
                if (campaignType?.shouldCheckForSddVerification() == true) {
                    tryToVerifyUserForSdd(state, campaignType)
                } else Single.just(state)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onSuccess = {
                    when (it.progressToKycNextStep) {
                        is KycNextStepDecision.NextStep.Questionnaire ->
                            view.continueToQuestionnaire(it.progressToKycNextStep.questionnaire, it.countryCode)
                        KycNextStepDecision.NextStep.Tier1Complete -> view.tier1Complete()
                        KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo ->
                            view.continueToTier2MoreInfoNeeded(it.countryCode)
                        KycNextStepDecision.NextStep.Veriff -> view.continueToVeriffSplash(it.countryCode)
                        KycNextStepDecision.NextStep.SDDComplete -> view.onSddVerified()
                    }.exhaustive
                },
                onError = {
                    when ((it as? NabuApiException?)?.getErrorCode()) {
                        NabuErrorCodes.InvalidPostcode -> view.showInvalidPostcode()
                        else -> view.showErrorSnackbar(R.string.kyc_address_error_saving)
                    }
                }
            )
    }

    private fun tryToVerifyUserForSdd(state: State, campaignType: CampaignType): Single<State> {
        return custodialWalletManager.isSimplifiedDueDiligenceEligible().doOnSuccess {
            if (it) {
                analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
            }
        }.flatMap {
            if (!it) {
                Single.just(state)
            } else {
                PollService(custodialWalletManager.fetchSimplifiedDueDiligenceUserState()) { sddState ->
                    sddState.stateFinalised
                }.start(timerInSec = 1, retries = 10).map { sddState ->
                    if (sddState.value.isVerified) {
                        if (shouldNotContinueToNextKycTier(state, campaignType)) {
                            state.copy(progressToKycNextStep = KycNextStepDecision.NextStep.SDDComplete)
                        } else {
                            state
                        }
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun shouldNotContinueToNextKycTier(
        state: State,
        campaignType: CampaignType,
    ): Boolean {
        return state.progressToKycNextStep < KycNextStepDecision.NextStep.SDDComplete ||
            campaignType == CampaignType.SimpleBuy
    }

    private fun addAddress(address: AddressModel): Completable = fetchOfflineToken.flatMapCompletable {
        nabuDataManager.addAddress(
            it,
            address.firstLine,
            address.secondLine,
            address.city,
            address.state,
            address.postCode,
            address.country
        ).subscribeOn(Schedulers.io())
    }

    private fun updateNabuData(): Completable = nabuUserSync.syncUser()

    private fun getCountryName(countryCode: String): Maybe<String> = countryCodeSingle
        .map { it.entries.first { (_, value) -> value == countryCode }.key }
        .toMaybe()

    private fun enableButtonIfComplete(addressModel: AddressModel) {
        if (addressModel.country.equals("US", ignoreCase = true)) {
            view.setButtonEnabled(
                addressModel.firstLine.isNotEmpty() &&
                    addressModel.city.isNotEmpty() &&
                    addressModel.state.isNotEmpty() &&
                    addressModel.postCode.isNotEmpty()
            )
        } else {
            view.setButtonEnabled(
                addressModel.firstLine.isNotEmpty() &&
                    addressModel.city.isNotEmpty() &&
                    addressModel.postCode.isNotEmpty()
            )
        }
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }

    private fun AddressModel.containsData(): Boolean =
        firstLine.isNotEmpty() ||
            !secondLine.isNullOrEmpty() ||
            city.isNotEmpty() ||
            state.isNotEmpty() ||
            postCode.isNotEmpty()
}

private fun CampaignType.shouldCheckForSddVerification(): Boolean =
    this == CampaignType.SimpleBuy || this == CampaignType.None
