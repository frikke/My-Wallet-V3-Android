package piuk.blockchain.android.ui.kyc.veriffsplash

import app.cash.turbine.test
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.SimplifiedDueDiligenceUserState
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.preferences.SessionPrefs
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.veriff.VeriffApplicantAndToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.getBlankNabuUser
import retrofit2.HttpException
import retrofit2.Response

class VeriffSplashModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val custodialWalletManager: CustodialWalletManager = mockk {
        every { fetchSimplifiedDueDiligenceUserState() } returns Single.just(
            SimplifiedDueDiligenceUserState(isVerified = true, stateFinalised = true)
        )
    }
    private val userService: UserService = mockk {
        every { getUser() } returns Single.just(getBlankNabuUser(KycState.Pending))
    }
    private val nabuDataManager: NabuDataManager = mockk()
    private val kycTiersStore: KycTiersStore = mockk()
    private val analytics: Analytics = mockk(relaxed = true)
    private val sessionPrefs: SessionPrefs = mockk(relaxed = true)

    private val subject = VeriffSplashModel(
        custodialWalletManager = custodialWalletManager,
        userService = userService,
        nabuDataManager = nabuDataManager,
        kycTiersStore = kycTiersStore,
        analytics = analytics,
        sessionPrefs = sessionPrefs
    )

    @Test
    fun onViewReady_happyPath_displayDocsView() = runTest {
        // Arrange
        val supportedDocumentsResult = PublishSubject.create<List<SupportedDocuments>>()
        every { nabuDataManager.getSupportedDocuments(COUNTRY_CODE) } returns supportedDocumentsResult.firstOrError()
        val startVeriffSessionResult = PublishSubject.create<VeriffApplicantAndToken>()
        every { nabuDataManager.startVeriffSession() } returns startVeriffSessionResult.firstOrError()

        // Act
        subject.viewCreated(Args(COUNTRY_CODE))

        // Assert
        subject.viewState.test {
            awaitItem().apply {
                isLoading shouldBeEqualTo true
                continueButtonState shouldBeEqualTo ButtonState.Disabled
            }
            supportedDocumentsResult.onNext(SUPPORTED_DOCS)
            startVeriffSessionResult.onNext(APPLICANT_TOKEN)
            expectMostRecentItem().apply {
                isLoading shouldBeEqualTo false
                continueButtonState shouldBeEqualTo ButtonState.Enabled
                supportedDocuments shouldBeEqualTo SUPPORTED_DOCS.toSortedSet()
            }
        }
    }

    @Test
    fun onViewReady_pre_IDV_fail_displayUnavailableView() = runTest {
        val body = Response.error<VeriffApplicantAndToken>(
            406,
            ResponseBody.create(
                null,
                "{\"message\":\"Totes Nope\"}"
            )
        )
        val httpError = NabuApiExceptionFactory.fromResponseBody(HttpException(body))

        // Assert
        subject.viewState.test {
            // Arrange
            val supportedDocumentsResult = PublishSubject.create<List<SupportedDocuments>>()
            every { nabuDataManager.getSupportedDocuments(COUNTRY_CODE) } returns supportedDocumentsResult.firstOrError()
            val startVeriffSessionResult = PublishSubject.create<VeriffApplicantAndToken>()
            every { nabuDataManager.startVeriffSession() } returns startVeriffSessionResult.firstOrError()

            // Act
            subject.viewCreated(Args(COUNTRY_CODE))

            awaitItem().apply {
                isLoading shouldBeEqualTo true
                continueButtonState shouldBeEqualTo ButtonState.Disabled
            }
            supportedDocumentsResult.onNext(SUPPORTED_DOCS)
            startVeriffSessionResult.onError(httpError)
            verify { sessionPrefs.devicePreIDVCheckFailed = true }
            expectMostRecentItem().apply {
                isLoading shouldBeEqualTo false
                continueButtonState shouldBeEqualTo ButtonState.Disabled
                supportedDocuments shouldBeEqualTo sortedSetOf()
                error shouldBeEqualTo null
            }
        }
        subject.navigationEventFlow.test {
            // Arrange
            every { nabuDataManager.getSupportedDocuments(COUNTRY_CODE) } returns Single.just(SUPPORTED_DOCS)
            every { nabuDataManager.startVeriffSession() } returns Single.error(httpError)

            // Act
            subject.viewCreated(Args(COUNTRY_CODE))

            awaitItem() shouldBeEqualTo Navigation.TierCurrentState(KycState.Rejected, false)
        }
    }

    companion object {
        private const val COUNTRY_CODE = "UK"

        private val SUPPORTED_DOCS = listOf(
            SupportedDocuments.PASSPORT,
            SupportedDocuments.DRIVING_LICENCE
        )

        private val APPLICANT_TOKEN = VeriffApplicantAndToken(
            applicantId = "WHATEVER",
            token = "ANOTHER_TOKEN"
        )
    }
}
