package piuk.blockchain.android.ui.settings.security.password

import com.blockchain.core.auth.AuthDataManager
import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.rxjava3.core.Single
import kotlin.math.roundToInt
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class PasswordChangeInteractor internal constructor(
    private val payloadManager: PayloadDataManager,
    private val authDataManager: AuthDataManager,
    private val pinRepository: PinRepository
) {
    private val tempWalletPassword
        get() = payloadManager.tempPassword

    fun checkPasswordValidity(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ): Single<PasswordChangeIntent> {
        return when {
            currentPassword == newPassword -> Single.just(
                PasswordChangeIntent.UpdateErrorState(PasswordChangeError.USING_SAME_PASSWORDS)
            )
            currentPassword != tempWalletPassword -> Single.just(
                PasswordChangeIntent.UpdateErrorState(PasswordChangeError.CURRENT_PASSWORD_WRONG)
            )
            newPassword != newPasswordConfirmation -> Single.just(
                PasswordChangeIntent.UpdateErrorState(PasswordChangeError.NEW_PASSWORDS_DONT_MATCH)
            )
            newPasswordConfirmation.length < 4 || newPasswordConfirmation.length > 255 -> Single.just(
                PasswordChangeIntent.UpdateErrorState(PasswordChangeError.NEW_PASSWORD_INVALID_LENGTH)
            )
            PasswordUtil.getStrength(newPasswordConfirmation).roundToInt() < 50 -> Single.just(
                PasswordChangeIntent.UpdateErrorState(PasswordChangeError.NEW_PASSWORD_TOO_WEAK)
            )
            else -> {
                payloadManager.updatePassword(password = newPasswordConfirmation).then {
                    authDataManager.createPin(newPasswordConfirmation, pinRepository.pin)
                }
                    .then { authDataManager.verifyCloudBackup() }
                    .thenSingle {
                        Single.just(PasswordChangeIntent.UpdateViewState(PasswordViewState.PasswordUpdated))
                    }
            }
        }
    }
}
