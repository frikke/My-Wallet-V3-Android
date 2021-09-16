package piuk.blockchain.android.ui.kyc.autocomplete

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

val kycAutocompleteModule = module {

    scope(payloadScopeQualifier) {

        factory {
            KycAutocompleteAddressModel(
                initialState = KycAutocompleteAddressState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            KycAutocompleteAddressInteractor()
        }
    }
}