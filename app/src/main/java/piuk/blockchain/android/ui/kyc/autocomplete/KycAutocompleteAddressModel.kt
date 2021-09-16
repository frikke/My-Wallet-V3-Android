package piuk.blockchain.android.ui.kyc.autocomplete

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorTypes
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

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
    override fun performAction(previousState: KycAutocompleteAddressState, intent: KycAutocompleteAddressIntents): Disposable? {
        return null
    }

//    private fun setAddressSearchText(
//        addressSearchText: String
//    ): Disposable {
////        return interactor.setNewPassword(password = password)
////            .subscribeBy(
////                onComplete = { resetKycOrContinue(shouldResetKyc) },
////                onError = { throwable ->
////                    Timber.e(throwable)
////                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
////                }
////            )
//    }


}