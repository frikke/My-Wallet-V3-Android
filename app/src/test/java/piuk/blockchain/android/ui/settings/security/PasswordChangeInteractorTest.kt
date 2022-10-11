package piuk.blockchain.android.ui.settings.security

import com.blockchain.core.auth.AuthDataManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeError
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeIntent
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeInteractor
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordViewState
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class PasswordChangeInteractorTest {

    private lateinit var interactor: PasswordChangeInteractor

    private val payloadManager: PayloadDataManager = mock()
    private val authDataManager: AuthDataManager = mock()
    private val pinRepository: PinRepository = mock()
    private val currentPassword = "blockchain@123"

    @Before
    fun setup() {
        interactor = PasswordChangeInteractor(
            payloadManager = payloadManager,
            authDataManager = authDataManager,
            pinRepository = pinRepository
        )
        whenever(payloadManager.tempPassword).thenReturn(currentPassword)
    }

    @Test
    fun `same new and old passwords errors`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blockchain@123"
        val confirmationPassword = "blockchain@123"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.USING_SAME_PASSWORDS
        }
    }

    @Test
    fun `incorrect current password errors`() {
        val existingPassword = "blockchain"
        val newPassword = "blockchain@123"
        val confirmationPassword = "blockchain@123"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.CURRENT_PASSWORD_WRONG
        }
    }

    @Test
    fun `mismatched new password and confirmation errors`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blockchain@1234"
        val confirmationPassword = "blockchain@123"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.NEW_PASSWORDS_DONT_MATCH
        }
    }

    @Test
    fun `confirmation password too short errors`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blo"
        val confirmationPassword = "blo"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.NEW_PASSWORD_INVALID_LENGTH
        }
    }

    @Test
    fun `confirmation password too long errors`() {
        val existingPassword = "blockchain@123"
        val newPassword =
            "THIS STRING IS 256 CHARACTERS xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            newPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.NEW_PASSWORD_INVALID_LENGTH
        }
    }

    @Test
    fun `password strength too low errors`() {
        val existingPassword = "blockchain@123"
        val newPassword = "block"
        val confirmationPassword = "block"

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateErrorState &&
                it.errorState == PasswordChangeError.NEW_PASSWORD_TOO_WEAK
        }
    }

    @Test
    fun `valid password updates correctly`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blockchain@12345"
        val confirmationPassword = "blockchain@12345"

        val pin = "1234"
        whenever(pinRepository.pin).thenReturn(pin)
        whenever(authDataManager.createPin(confirmationPassword, pin)).thenReturn(Completable.complete())
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())
        whenever(payloadManager.updatePassword(newPassword)).thenReturn(Completable.complete())

        val test = interactor.checkPasswordValidity(
            existingPassword,
            newPassword,
            confirmationPassword
        ).test()

        test.assertValue {
            it is PasswordChangeIntent.UpdateViewState &&
                it.viewState == PasswordViewState.PasswordUpdated
        }

        verify(payloadManager).tempPassword
        verify(pinRepository).pin
        verify(authDataManager).createPin(confirmationPassword, pin)
        verify(authDataManager).verifyCloudBackup()
        verify(payloadManager).updatePassword(newPassword)

        verifyNoMoreInteractions(payloadManager)
        verifyNoMoreInteractions(pinRepository)
        verifyNoMoreInteractions(authDataManager)
    }
}
