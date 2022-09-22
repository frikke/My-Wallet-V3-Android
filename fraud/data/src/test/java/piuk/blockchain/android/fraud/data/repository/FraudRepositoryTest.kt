package piuk.blockchain.android.fraud.data.repository

import com.blockchain.api.services.SessionService
import com.blockchain.api.session.data.GenerateSessionResponse
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.androidcore.data.api.interceptors.SessionId

@OptIn(ExperimentalCoroutinesApi::class)
class FraudRepositoryTest {

    private val sessionService = mockk<SessionService>(relaxed = true)
    private val sessionId = mockk<SessionId>(relaxed = true)
    private val sessionIdFeatureFlag = mockk<FeatureFlag>(relaxed = true)

    private val generateSessionResponse = GenerateSessionResponse("id")

    private lateinit var subject: FraudService

    @Before
    fun setUp() {
        subject = FraudRepository(
            coroutineScope = TestScope(),
            dispatcher = UnconfinedTestDispatcher(),
            sessionService = sessionService,
            sessionId = sessionId,
            sessionIdFeatureFlag = sessionIdFeatureFlag
        )
    }

    @Test
    fun `GIVEN xSessionId is VALID, WHEN updateSessionId() is called, THEN sessionId should be set`() = runTest {
        // Arrange
        coEvery { sessionIdFeatureFlag.coEnabled() } returns true
        coEvery { sessionService.getSessionId() } returns Outcome.Success(generateSessionResponse)

        // Act
        subject.updateSessionId()

        // Assert
        coVerify {
            sessionId.clearSessionId()
            sessionId.setSessionId(generateSessionResponse.xSessionId)
        }
    }

    @Test
    fun `GIVEN xSessionId is INVALID, WHEN updateSessionId() is called, THEN sessionId should be cleared`() = runTest {
        // Arrange
        coEvery { sessionIdFeatureFlag.coEnabled() } returns true
        coEvery { sessionService.getSessionId() } returns Outcome.Failure(Exception())

        // Act
        subject.updateSessionId()

        // Assert
        coVerify {
            sessionId.clearSessionId()
        }
        coVerify(exactly = 0) {
            sessionId.setSessionId(any())
        }
    }

    @Test
    fun `GIVEN FeatureFlag is off, WHEN updateSessionId() is called, THEN sessionId should be cleared`() = runTest {
        // Arrange
        coEvery { sessionIdFeatureFlag.coEnabled() } returns false

        // Act
        subject.updateSessionId()

        // Assert
        coVerify {
            sessionId.clearSessionId()
        }
        coVerify(exactly = 0) {
            sessionId.setSessionId(any())
        }
    }
}
