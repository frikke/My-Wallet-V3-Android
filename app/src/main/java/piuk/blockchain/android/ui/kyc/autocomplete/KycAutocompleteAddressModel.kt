package piuk.blockchain.android.ui.kyc.autocomplete

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class KycAutocompleteAddressModel(
    initialState: KycAutocompleteAddressState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: KycAutocompleteAddressInteractor
) : MviModel<KycAutocompleteAddressState, KycAutocompleteAddressIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(
        previousState: KycAutocompleteAddressState,
        intent: KycAutocompleteAddressIntents
    ): Disposable? {
        when (intent) {
            is KycAutocompleteAddressIntents.SelectAddress -> {
                interactor.getAddressDetails(intent.selectedAddress.placeId)
                    .subscribeBy(
                        onSuccess = {
                            process(KycAutocompleteAddressIntents.NavigateToAddress(it))
                            process(KycAutocompleteAddressIntents.ClearNavigation)
                        },
                        onError = { }
                    )
            }
            is KycAutocompleteAddressIntents.UpdateSearchText -> {
                return interactor.searchForAddresses(intent.addressSearchText, intent.countryCode)
                    .subscribeBy(
                        onSuccess = {
                            process(KycAutocompleteAddressIntents.UpdateAddresses(it))
                        },
                        onError = { }
                    )
            }
        }

        return null
    }
}