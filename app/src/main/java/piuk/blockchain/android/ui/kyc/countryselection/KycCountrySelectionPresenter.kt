package piuk.blockchain.android.ui.kyc.countryselection

import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.countryselection.models.CountrySelectionState
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel
import piuk.blockchain.android.ui.kyc.countryselection.util.toDisplayList
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

internal class KycCountrySelectionPresenter(
    private val eligibilityService: EligibilityService
) : BasePresenter<KycCountrySelectionView>() {

    private val usCountryCode = "US"

    private val countriesList by unsafeLazy {
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            eligibilityService.getCountriesList(GetRegionScope.None)
        }.cache()
    }

    private val usStatesList by unsafeLazy {
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            eligibilityService.getStatesList(usCountryCode, GetRegionScope.None)
        }.cache()
    }

    private fun getRegionList() =
        if (view.regionType == RegionType.Country) countriesList else usStatesList

    override fun onViewReady() {
        compositeDisposable +=
            getRegionList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.renderUiState(CountrySelectionState.Loading) }
                .doOnError {
                    view.renderUiState(
                        CountrySelectionState.Error(R.string.kyc_country_selection_connection_error)
                    )
                }
                .doOnSuccess { view.renderUiState(CountrySelectionState.Data(it.toDisplayList())) }
                .subscribeBy(onError = { Timber.e(it) })
    }

    internal fun onRegionSelected(
        countryDisplayModel: CountryDisplayModel
    ) {
        compositeDisposable +=
            getRegionList()
                .flatMapMaybe { regions ->
                    Maybe.just(regions)
                }
                .filter {
                    it.isKycAllowed(countryDisplayModel) &&
                        !countryDisplayModel.requiresStateSelection()
                }
                .subscribeBy(
                    onSuccess = {
                        view.continueFlow(
                            countryDisplayModel.countryCode,
                            countryDisplayModel.state,
                            if (countryDisplayModel.isState) countryDisplayModel.name else null
                        )
                    },
                    onComplete = {
                        when {
                            // Not found, is US, must select state
                            countryDisplayModel.requiresStateSelection() -> view.requiresStateSelection()
                            // Not found, invalid
                            else -> view.invalidCountry(countryDisplayModel)
                        }
                    },
                    onError = {
                        throw IllegalStateException("Region list should already be cached")
                    }
                )
    }

    private fun List<Region>.isKycAllowed(countryDisplayModel: CountryDisplayModel): Boolean =
        this.any { it.isMatchingRegion(countryDisplayModel) && it.isKycAllowed }

    private fun Region.isMatchingRegion(countryDisplayModel: CountryDisplayModel): Boolean = when (this) {
        is Region.Country -> !countryDisplayModel.isState && this.countryCode == countryDisplayModel.countryCode
        is Region.State -> countryDisplayModel.isState && this.stateCode == countryDisplayModel.state
    }

    private fun CountryDisplayModel.requiresStateSelection(): Boolean =
        this.countryCode.equals(usCountryCode, ignoreCase = true) && !this.isState

    internal fun onRequestCancelled() {
        compositeDisposable.clear()
    }
}
