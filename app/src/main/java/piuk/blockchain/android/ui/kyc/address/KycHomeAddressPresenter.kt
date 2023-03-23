package piuk.blockchain.android.ui.kyc.address

import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.addressverification.ui.AddressVerificationSavingError
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.thenSingle
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.SortedMap
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.android.ui.base.BasePresenter
import timber.log.Timber

interface KycNextStepDecision {

    sealed class NextStep(val order: Int) : Comparable<NextStep> {
        object Tier1Complete : NextStep(0)
        object Tier2ContinueTier1NeedsMoreInfo : NextStep(2)
        data class Questionnaire(val questionnaire: com.blockchain.domain.dataremediation.model.Questionnaire) :
            NextStep(3)
        object Veriff : NextStep(4)

        override fun compareTo(other: NextStep): Int = this.order - other.order
    }

    fun nextStep(): Single<NextStep>
}

class KycHomeAddressPresenter(
    private val nabuDataManager: NabuDataManager,
    private val eligibilityService: EligibilityService,
    private val nabuUserSync: NabuUserSync,
    private val kycNextStepDecision: KycHomeAddressNextStepDecision,
    private val kycTiersStore: KycTiersStore,
) : BasePresenter<KycHomeAddressView>() {

    val countryCodeSingle: Single<SortedMap<String, String>> by unsafeLazy {
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            eligibilityService.getCountriesList(GetRegionScope.None)
        }.subscribeOn(Schedulers.io())
            .map { list ->
                list.associateBy({ it.name }, { it.countryCode })
                    .toSortedMap()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .cache()
    }

    private data class State(
        val progressToKycNextStep: KycNextStepDecision.NextStep,
        val countryCode: String,
    )

    internal fun onContinueClicked(address: AddressDetails) {
        compositeDisposable += addAddress(address).toSingle { address.countryIso }
            .flatMap { countryCode ->
                kycTiersStore.markAsStale()
                nabuUserSync.syncUser().thenSingle { Single.just(countryCode) }
            }
            .map { countryCode ->
                State(
                    progressToKycNextStep = KycNextStepDecision.NextStep.Tier1Complete,
                    countryCode = countryCode
                )
            }
            .zipWith(kycNextStepDecision.nextStep())
            .map { (x, progress) -> x.copy(progressToKycNextStep = progress) }
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
                    }.exhaustive
                },
                onError = {
                    when ((it as? NabuApiException?)?.getErrorCode()) {
                        NabuErrorCodes.InvalidPostcode ->
                            view.showErrorWhileSaving(AddressVerificationSavingError.InvalidPostCode)
                        else -> view.showErrorWhileSaving(AddressVerificationSavingError.Unknown(it.message))
                    }
                }
            )
    }

    private fun addAddress(address: AddressDetails): Completable =
        nabuDataManager.addAddress(
            address.firstLine,
            address.secondLine,
            address.city,
            address.stateIso,
            address.postCode,
            address.countryIso
        ).subscribeOn(Schedulers.io())

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}
