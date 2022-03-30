package piuk.blockchain.android.ui.reset.password

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ResetPasswordModelTest {

    private lateinit var model: ResetPasswordModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: ResetPasswordInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = ResetPasswordModel(
            initialState = ResetPasswordState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `recover account and reset kyc successfully`() {
        val email = "email"
        val password = "password"
        val userId = "user_id"
        val recoveryToken = "recovery_token"
        val walletName = "wallet_name"

        whenever(interactor.createWalletForAccount(email, password, walletName)).thenReturn(Completable.complete())
        whenever(interactor.recoverAccount(userId, recoveryToken)).thenReturn(Completable.complete())
        whenever(interactor.resetUserKyc()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.CreateWalletForAccount(
                email,
                password,
                userId,
                recoveryToken,
                walletName,
                true
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                email = email,
                password = password,
                userId = userId,
                recoveryToken = recoveryToken,
                walletName = walletName,
                status = ResetPasswordStatus.CREATE_WALLET
            ),
            ResetPasswordState(
                email = email,
                password = password,
                userId = userId,
                recoveryToken = recoveryToken,
                walletName = walletName,
                status = ResetPasswordStatus.RECOVER_ACCOUNT
            ),
            ResetPasswordState(
                email = email,
                password = password,
                userId = userId,
                recoveryToken = recoveryToken,
                walletName = walletName,
                status = ResetPasswordStatus.RESET_KYC
            ),
            ResetPasswordState(
                email = email,
                password = password,
                userId = userId,
                recoveryToken = recoveryToken,
                walletName = walletName,
                status = ResetPasswordStatus.SHOW_SUCCESS
            )
        )
    }

    @Test
    fun `set password successfully`() {
        val password = "password"

        whenever(interactor.setNewPassword(password)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.SetNewPassword(
                password,
                false
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SET_PASSWORD
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SHOW_SUCCESS
            )
        )
    }

    @Test
    fun `fail to set new password should show error`() {
        val password = "password"

        whenever(interactor.setNewPassword(password)).thenReturn(
            Completable.error(
                Exception()
            )
        )

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.SetNewPassword(
                password,
                false
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SET_PASSWORD
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SHOW_ERROR
            )
        )
    }

    @Test
    fun `fail to reset kyc when resetting password should show error`() {
        val password = "password"

        whenever(interactor.setNewPassword(password)).thenReturn(
            Completable.complete()
        )
        whenever(interactor.resetUserKyc()).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.SetNewPassword(
                password,
                true
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SET_PASSWORD
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.RESET_KYC
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SHOW_RESET_KYC_FAILED
            )
        )
    }

    @Test
    fun `reset kyc is already in progress when resetting password, continue`() {
        val password = "password"

        whenever(interactor.setNewPassword(password)).thenReturn(
            Completable.complete()
        )
        whenever(interactor.resetUserKyc()).thenReturn(
            Completable.error(
                NabuApiExceptionFactory.fromResponseBody(
                    HttpException(
                        Response.error<Unit>(
                            409,
                            KYC_IN_PROGRESS_ERROR_RESPONSE.toResponseBody(JSON_HEADER.toMediaTypeOrNull())
                        )
                    )
                )
            )
        )

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.SetNewPassword(
                password,
                true
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SET_PASSWORD
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.RESET_KYC
            ),
            ResetPasswordState(
                password = password,
                status = ResetPasswordStatus.SHOW_SUCCESS
            )
        )
    }

    @Test
    fun `fail to create wallet when resetting account should show error`() {
        val password = "password"
        val email = "email"
        val walletName = "walletName"
        val userId = "1234"
        val token = "4321"

        whenever(interactor.createWalletForAccount(email, password, walletName)).thenReturn(
            Completable.error(Exception())
        )

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.CreateWalletForAccount(
                email,
                password,
                userId,
                token,
                walletName,
                false
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                password = password,
                email = email,
                walletName = walletName,
                userId = userId,
                recoveryToken = token,
                status = ResetPasswordStatus.CREATE_WALLET
            ),
            ResetPasswordState(
                password = password,
                email = email,
                walletName = walletName,
                userId = userId,
                recoveryToken = token,
                status = ResetPasswordStatus.SHOW_WALLET_CREATION_FAILED
            )
        )
    }

    @Test
    fun `fail to create account when resetting should show error`() {
        val userId = "1234"
        val token = "4321"

        whenever(interactor.recoverAccount(userId, token)).thenReturn(
            Completable.error(Exception())
        )

        val testState = model.state.test()
        model.process(
            ResetPasswordIntents.RecoverAccount(
                userId,
                token,
                false
            )
        )

        testState.assertValues(
            ResetPasswordState(),
            ResetPasswordState(
                userId = userId,
                recoveryToken = token,
                status = ResetPasswordStatus.RECOVER_ACCOUNT
            ),
            ResetPasswordState(
                userId = userId,
                recoveryToken = token,
                status = ResetPasswordStatus.SHOW_ACCOUNT_RESET_FAILED
            )
        )
    }

    companion object {
        private const val JSON_HEADER = "application/json"
        private const val KYC_IN_PROGRESS_ERROR_RESPONSE =
            "{\"type\":\"CONFLICT\",\"description\":\"User reset in progress\"}"
    }
}
