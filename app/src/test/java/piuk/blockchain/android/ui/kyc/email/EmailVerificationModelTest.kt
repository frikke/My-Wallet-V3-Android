package piuk.blockchain.android.ui.kyc.email

import app.cash.turbine.test
import com.blockchain.core.settings.Email
import com.blockchain.core.settings.EmailSyncUpdater
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.email.entry.Args
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationError
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationIntent
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerificationModel
import piuk.blockchain.android.ui.kyc.email.entry.Navigation

class EmailVerificationModelTest {

    private lateinit var model: EmailVerificationModel

    private val emailUpdater: EmailSyncUpdater = mockk(relaxed = true)
    private val getUserStore: GetUserStore = mockk(relaxed = true)

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    @Before
    fun setUp() {
        model = EmailVerificationModel(
            emailUpdater = emailUpdater,
            getUserStore = getUserStore
        )
    }

    @Test
    fun `on view created it should fetch email and update state`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)

        model.viewState.test {
            expectMostRecentItem()

            model.viewCreated(Args(emailMustBeValidated = true))
            awaitItem().apply {
                email shouldBeEqualTo EMAIL
                isVerified shouldBeEqualTo false
            }

            expectNoEvents()
            every { emailUpdater.email() } returns Single.error(Exception("some error"))
            model.viewCreated(Args(emailMustBeValidated = true))
            awaitItem().error shouldBeEqualTo EmailVerificationError.Generic("some error")
        }
    }

    @Test
    fun `on view created should start polling for verification if the email is not yet verified`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)
        val pollingResult = CompletableDeferred<Outcome<Exception, Email>>()
        coEvery { emailUpdater.pollForEmailVerification(any(), any()) } coAnswers { pollingResult.await() }

        model.viewState.test {
            model.viewCreated(Args(emailMustBeValidated = true))
            expectMostRecentItem()

            pollingResult.complete(Outcome.Success(EMAIL_VERIFIED))
            awaitItem().apply {
                email shouldBeEqualTo EMAIL
                isVerified shouldBeEqualTo true
            }
            verify { getUserStore.markAsStale() }
        }
    }

    @Test
    fun `on view created if email must be validated it should resend verification email`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)
        val resendEmailResult = PublishSubject.create<Email>()
        every { emailUpdater.resendEmail(EMAIL) } returns resendEmailResult.firstOrError()

        model.viewState.test {
            model.viewCreated(Args(emailMustBeValidated = true))
            expectMostRecentItem() // ignore emission after emailUpdater.email() returns, updating the viewState.email
            verify { emailUpdater.resendEmail(EMAIL) }
            resendEmailResult.onNext(EMAIL_UNVERIFIED)
            // the resend on viewCreated should show resend email confirmation
            expectNoEvents()

            every { emailUpdater.resendEmail(EMAIL) } returns Single.error(Exception("some error"))
            model.viewCreated(Args(emailMustBeValidated = true))
            // the resend on viewCreated should not show an error
            expectNoEvents()
        }
    }

    @Test
    fun `on edit email clicked it should navigate`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)

        model.viewCreated(Args(emailMustBeValidated = true))
        model.navigationEventFlow.test {
            model.onIntent(EmailVerificationIntent.EditEmailClicked)
            awaitItem() shouldBeEqualTo Navigation.EditEmailSheet(EMAIL)
        }
    }

    @Test
    fun `on resend email clicked it should resend verification email`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)
        every { emailUpdater.resendEmail(EMAIL) } returns Single.just(EMAIL_UNVERIFIED)

        model.viewState.test {
            model.viewCreated(Args(emailMustBeValidated = true))
            expectMostRecentItem()
            model.onIntent(EmailVerificationIntent.ResendEmailClicked)
            verify { emailUpdater.resendEmail(EMAIL) }
            awaitItem().showResendEmailConfirmation shouldBeEqualTo true
            model.onIntent(EmailVerificationIntent.ShowResendEmailConfirmationHandled)
            awaitItem().showResendEmailConfirmation shouldBeEqualTo false
            expectNoEvents()

            every { emailUpdater.resendEmail(EMAIL) } returns Single.error(Exception("some error"))
            model.onIntent(EmailVerificationIntent.ResendEmailClicked)
            awaitItem().error shouldBeEqualTo EmailVerificationError.Generic("some error")
            expectNoEvents()
        }
    }

    @Test
    fun `on email changed it should update email`() = runTest {
        model.viewCreated(Args(emailMustBeValidated = true))
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)
        val successNewEmail = "newemail@email.com"
        every { emailUpdater.updateEmailAndSync(successNewEmail) } returns Single.just(Email(successNewEmail, false))

        model.viewState.test {
            expectMostRecentItem()
            model.onIntent(EmailVerificationIntent.OnEmailChanged(successNewEmail))
            verify { emailUpdater.updateEmailAndSync(successNewEmail) }
            awaitItem().apply {
                email shouldBeEqualTo successNewEmail
                showResendEmailConfirmation shouldBeEqualTo true
            }
            expectNoEvents()

            val failureNewEmail = "anothernewemail@email.com"
            every { emailUpdater.updateEmailAndSync(failureNewEmail) } returns Single.error(Exception("some error"))
            model.onIntent(EmailVerificationIntent.OnEmailChanged(failureNewEmail))
            verify { emailUpdater.updateEmailAndSync(failureNewEmail) }
            awaitItem().apply {
                email shouldBeEqualTo successNewEmail
                error shouldBeEqualTo EmailVerificationError.Generic("some error")
            }
            expectNoEvents()
        }
    }

    @Test
    fun `on start polling for verification it should start polling and update state once verified`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)
        val pollingResult = CompletableDeferred<Outcome<Exception, Email>>()
        coEvery { emailUpdater.pollForEmailVerification(any(), any()) } coAnswers { pollingResult.await() }

        model.viewState.test {
            model.viewCreated(Args(emailMustBeValidated = true))
            model.onIntent(EmailVerificationIntent.StopPollingForVerification)
            model.onIntent(EmailVerificationIntent.StartPollingForVerification)
            expectMostRecentItem()

            pollingResult.complete(Outcome.Success(EMAIL_VERIFIED))

            awaitItem().apply {
                email shouldBeEqualTo EMAIL
                isVerified shouldBeEqualTo true
            }
            verify { getUserStore.markAsStale() }
        }
    }

    @Test
    fun `on stop polling for verification it should stop polling`() = runTest {
        every { emailUpdater.email() } returns Single.just(EMAIL_UNVERIFIED)

        model.viewState.test {
            model.viewCreated(Args(emailMustBeValidated = true))
            expectMostRecentItem()
            model.onIntent(EmailVerificationIntent.StartPollingForVerification)
            model.onIntent(EmailVerificationIntent.StopPollingForVerification)
            expectNoEvents()
            every { emailUpdater.email() } returns Single.just(EMAIL_VERIFIED)
            delay(10_000)
            expectNoEvents()
        }
    }

    companion object {
        private val EMAIL = "address@email.com"
        private val EMAIL_VERIFIED = Email(EMAIL, true)
        private val EMAIL_UNVERIFIED = Email(EMAIL, false)
    }
}
