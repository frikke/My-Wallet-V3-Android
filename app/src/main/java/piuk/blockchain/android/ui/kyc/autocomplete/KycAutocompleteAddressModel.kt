package piuk.blockchain.android.ui.kyc.autocomplete

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class KycAutocompleteAddressModel(
    initialState: KycAutocompleteAddressState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val interactor: KycAutocompleteAddressInteractor
) : MviModel<KycAutocompleteAddressState, KycAutocompleteAddressIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
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
                        onError = {
                            process(
                                KycAutocompleteAddressIntents.DisplayErrorToast(
                                    AutocompleteAddressToastType.SELECTED_ADDRESS_ERROR
                                )
                            )
                            process(KycAutocompleteAddressIntents.HideErrorToast)
                        }
                    )
            }
            is KycAutocompleteAddressIntents.UpdateSearchText -> {
                return interactor.searchForAddresses(intent.addressSearchText, intent.countryCode)
                    .subscribeBy(
                        onSuccess = {
                            process(KycAutocompleteAddressIntents.UpdateAddresses(it))
                        },
                        onError = {
                            process(
                                KycAutocompleteAddressIntents.DisplayErrorToast(
                                    AutocompleteAddressToastType.ADDRESSES_ERROR
                                )
                            )
                            process(KycAutocompleteAddressIntents.HideErrorToast)
                        }
                    )
            }
            KycAutocompleteAddressIntents.ClearNavigation,
            is KycAutocompleteAddressIntents.DisplayErrorToast,
            KycAutocompleteAddressIntents.HideErrorToast,
            is KycAutocompleteAddressIntents.NavigateToAddress,
            is KycAutocompleteAddressIntents.UpdateAddresses -> {
            }
        }

        return null
    }
}
