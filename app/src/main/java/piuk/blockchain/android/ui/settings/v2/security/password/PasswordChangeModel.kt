package piuk.blockchain.android.ui.settings.v2.security.password

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class PasswordChangeModel(
    initialState: PasswordChangeState,
    mainScheduler: Scheduler,
    private val interactor: PasswordChangeInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<PasswordChangeState, PasswordChangeIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: PasswordChangeState,
        intent: PasswordChangeIntent
    ): Disposable? =
        when (intent) {
            is PasswordChangeIntent.UpdatePassword ->
                interactor.checkPasswordValidity(
                    intent.currentPassword, intent.newPassword, intent.newPasswordConfirmation
                ).subscribeBy(
                    onSuccess = {
                        process(it)
                    }, onError = {
                    Timber.e("Error updating passwords: $it")
                    process(PasswordChangeIntent.UpdateErrorState(PasswordChangeError.UNKNOWN_ERROR))
                }
                )
            PasswordChangeIntent.ResetErrorState,
            PasswordChangeIntent.ResetViewState,
            is PasswordChangeIntent.UpdateErrorState,
            is PasswordChangeIntent.UpdateViewState -> null
        }.exhaustive
}
