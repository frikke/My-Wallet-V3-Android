package piuk.blockchain.android.ui.settings.v2.security.password

import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.rxjava3.core.Single
import kotlin.math.roundToInt
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

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
                payloadManager.tempPassword = newPasswordConfirmation
                authDataManager.createPin(newPasswordConfirmation, pinRepository.pin)
                    .andThen(authDataManager.verifyCloudBackup())
                    .andThen(payloadManager.syncPayloadWithServer())
                    .andThen(
                        Single.just(PasswordChangeIntent.UpdateViewState(PasswordViewState.PasswordUpdated))
                    )
            }
        }
    }
}
