package piuk.blockchain.android.ui.settings.security

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeError
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeIntent
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeInteractor
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeModel
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeState
import piuk.blockchain.android.ui.settings.security.password.PasswordViewState

class PasswordChangeModelTest {

    private lateinit var model: PasswordChangeModel
    private val defaultState = spy(PasswordChangeState())

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: PasswordChangeInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = PasswordChangeModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `update password succeeds`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blockchain@12345"
        val confirmationPassword = "blockchain@12345"

        whenever(interactor.checkPasswordValidity(existingPassword, newPassword, confirmationPassword)).thenReturn(
            Single.just(PasswordChangeIntent.UpdateViewState(PasswordViewState.PasswordUpdated))
        )

        val test = model.state.test()
        model.process(PasswordChangeIntent.UpdatePassword(existingPassword, newPassword, confirmationPassword))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.passwordViewState == PasswordViewState.CheckingPasswords
        }.assertValueAt(2) {
            it.passwordViewState == PasswordViewState.PasswordUpdated
        }
    }

    @Test
    fun `update password fails`() {
        val existingPassword = "blockchain@123"
        val newPassword = "blockchain@12345"
        val confirmationPassword = "blockchain@12345"

        whenever(interactor.checkPasswordValidity(existingPassword, newPassword, confirmationPassword)).thenReturn(
            Single.error(Exception())
        )

        val test = model.state.test()
        model.process(PasswordChangeIntent.UpdatePassword(existingPassword, newPassword, confirmationPassword))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.passwordViewState == PasswordViewState.CheckingPasswords
        }.assertValueAt(2) {
            it.errorState == PasswordChangeError.UNKNOWN_ERROR
        }
    }
}
