package piuk.blockchain.android.ui.kyc.email

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.settings.Email
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationIntent
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationInteractor
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationModel
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationState

class EmailVerificationModelTest {

    private val interactor: EmailVerificationInteractor = mock()

    private lateinit var model: EmailVerificationModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = EmailVerificationModel(
            interactor = interactor,
            uiScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock()
        )
    }

    @Test
    fun `for unverified email, it should return the unverified email and then the polling result`() {
        whenever(interactor.cancelPolling()).thenReturn(Completable.complete())
        whenever(interactor.fetchEmail()).thenReturn(Single.just(Email("address@example.com", false)))
        whenever(interactor.pollForEmailStatus()).thenReturn(Single.just(Email("address@example.com", true)))

        val statesTest = model.state.test()
        model.process(EmailVerificationIntent.StartEmailVerification)

        statesTest.assertValueAt(0, EmailVerificationState())
        statesTest.assertValueAt(
            1,
            EmailVerificationState(
                email = Email("address@example.com", false)
            )
        )
        statesTest.assertValueAt(
            2,
            EmailVerificationState(
                email = Email("address@example.com", true)
            )
        )
    }
}
