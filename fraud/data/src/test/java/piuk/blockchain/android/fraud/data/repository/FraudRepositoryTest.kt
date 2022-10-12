package piuk.blockchain.android.fraud.data.repository

import com.blockchain.api.fraud.data.FraudFlowsResponse
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.api.services.FraudRemoteService
import com.blockchain.api.services.SessionService
import com.blockchain.api.session.data.GenerateSessionResponse
import com.blockchain.enviroment.EnvironmentConfig
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
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudFlows
import piuk.blockchain.android.fraud.domain.service.FraudService

@OptIn(ExperimentalCoroutinesApi::class)
class FraudRepositoryTest {

    private val sessionService = mockk<SessionService>(relaxed = true)
    private val fraudService = mockk<FraudRemoteService>(relaxed = true)
    private val sessionInfo = mockk<SessionInfo>(relaxed = true)
    private val fraudFlows = mockk<FraudFlows>(relaxed = true)
    private val environmentConfig = mockk<EnvironmentConfig>(relaxed = true)
    private val sessionIdFeatureFlag = mockk<FeatureFlag>(relaxed = true)
    private val sardineFeatureFlag = mockk<FeatureFlag>(relaxed = true)

    private val generateSessionResponse = GenerateSessionResponse("id")
    private val fraudFlowsResponse = mockk<FraudFlowsResponse>(relaxed = true)

    private lateinit var subject: FraudService

    @Before
    fun setUp() {
        subject = FraudRepository(
            coroutineScope = TestScope(),
            dispatcher = UnconfinedTestDispatcher(),
            sessionService = sessionService,
            fraudService = fraudService,
            sessionInfo = sessionInfo,
            fraudFlows = fraudFlows,
            environmentConfig = environmentConfig,
            sessionIdFeatureFlag = sessionIdFeatureFlag,
            sardineFeatureFlag = sardineFeatureFlag
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
            sessionInfo.clearSessionId()
            sessionInfo.setSessionId(generateSessionResponse.xSessionId)
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
            sessionInfo.clearSessionId()
        }
        coVerify(exactly = 0) {
            sessionInfo.setSessionId(any())
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
            sessionInfo.clearSessionId()
        }
        coVerify(exactly = 0) {
            sessionInfo.setSessionId(any())
        }
    }

    @Test
    fun `GIVEN VALID fraudFlows, WHEN updateUnauthenticatedUserFlows() is called, THEN flows should be set`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns listOf(
                FraudFlowsResponse.FraudFlow("SIGNUP"), FraudFlowsResponse.FraudFlow("LOGIN")
            )
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateUnauthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearUnauthenticatedUserFlows()
                fraudFlows.addUnauthenticatedUserFlows(setOf(FraudFlow.LOGIN, FraudFlow.SIGNUP))
            }
        }

    @Test
    fun `GIVEN INVALID fraudFlows, WHEN updateUnauthenticatedUserFlows() is called, THEN flows should be filtered`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns listOf(
                FraudFlowsResponse.FraudFlow("bad_flow"), FraudFlowsResponse.FraudFlow("LOGIN")
            )
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateUnauthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearUnauthenticatedUserFlows()
                fraudFlows.addUnauthenticatedUserFlows(setOf(FraudFlow.LOGIN))
            }
        }

    @Test
    fun `GIVEN EMPTY fraudFlows, WHEN updateUnauthenticatedUserFlows() is called, THEN flows should be cleared`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns emptyList()
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateUnauthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearUnauthenticatedUserFlows()
            }
        }

    @Test
    fun `GIVEN fraudFlows ERROR, WHEN updateUnauthenticatedUserFlows() is called, THEN flows should be cleared`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudService.getFraudFlows() } returns Outcome.Failure(Exception())

            // Act
            subject.updateUnauthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearUnauthenticatedUserFlows()
            }
            coVerify(exactly = 0) {
                fraudFlows.addUnauthenticatedUserFlows(any())
            }
        }

    @Test
    fun `GIVEN VALID fraudFlows, WHEN updateAuthenticatedUserFlows() is called, THEN flows should be set`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns listOf(
                FraudFlowsResponse.FraudFlow("SIGNUP"), FraudFlowsResponse.FraudFlow("LOGIN")
            )
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateAuthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearAuthenticatedUserFlows()
                fraudFlows.addAuthenticatedUserFlows(setOf(FraudFlow.LOGIN, FraudFlow.SIGNUP))
            }
        }

    @Test
    fun `GIVEN INVALID fraudFlows, WHEN updateAuthenticatedUserFlows() is called, THEN flows should be filtered`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns listOf(
                FraudFlowsResponse.FraudFlow("bad_flow"), FraudFlowsResponse.FraudFlow("LOGIN")
            )
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateAuthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearAuthenticatedUserFlows()
                fraudFlows.addAuthenticatedUserFlows(setOf(FraudFlow.LOGIN))
            }
        }

    @Test
    fun `GIVEN EMPTY fraudFlows, WHEN updateAuthenticatedUserFlows() is called, THEN flows should be cleared`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudFlowsResponse.flows } returns emptyList()
            coEvery { fraudService.getFraudFlows() } returns Outcome.Success(fraudFlowsResponse)

            // Act
            subject.updateAuthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearAuthenticatedUserFlows()
            }
        }

    @Test
    fun `GIVEN fraudFlows ERROR, WHEN updateAuthenticatedUserFlows() is called, THEN flows should be cleared`() =
        runTest {
            // Arrange
            coEvery { sardineFeatureFlag.coEnabled() } returns true
            coEvery { fraudService.getFraudFlows() } returns Outcome.Failure(Exception())

            // Act
            subject.updateAuthenticatedUserFlows()

            // Assert
            coVerify {
                fraudFlows.clearAuthenticatedUserFlows()
            }
            coVerify(exactly = 0) {
                fraudFlows.addAuthenticatedUserFlows(any())
            }
        }
}
