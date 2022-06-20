package com.blockchain.nabu.datamanagers

import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.cache.UserCache
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class NabuDataUserProviderTest {

    private val authenticator = mockk<NabuAuthenticator>()
    private val userCache = mockk<UserCache>()
    private val userReporter = mockk<NabuUserReporter>()
    private val trust = mockk<DigitalTrust>()
    private val walletReporter = mockk<WalletReporter>()
    private val payloadDataManager = mockk<PayloadDataManager>()

    private val nabuDataUserProvider: NabuDataUserProvider = NabuDataUserProviderNabuDataManagerAdapter(
        authenticator = authenticator,
        userCache = userCache,
        userReporter = userReporter,
        trust = trust,
        walletReporter = walletReporter,
        payloadDataManager = payloadDataManager
    )

    private val guid = "guid"
    private val userObject: NabuUser = mockk()
    private val sessionToken = FakeNabuSessionTokenFactory.any

    @Before
    fun setUp() {
        every { authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<NabuUser>>()) } answers {
            firstArg<(NabuSessionTokenResponse) -> Single<NabuUser>>().invoke(sessionToken)
        }
        every { userCache.cached(sessionToken) } returns Single.just(userObject)

        every { payloadDataManager.guid } returns guid

        every { userReporter.reportUserId(any()) } just Runs
        every { userReporter.reportUser(any()) } just Runs
        every { trust.setUserId(any()) } just Runs
        every { walletReporter.reportWalletGuid(any()) } just Runs
    }

    @Test
    fun getUser() {
        // Act
        val testObserver = nabuDataUserProvider.getUser().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(userObject)
        verify(exactly = 1) { userCache.cached(sessionToken) }
        verify(exactly = 1) { walletReporter.reportWalletGuid(guid) }
        verify(exactly = 1) { userReporter.reportUser(userObject) }
        verify(exactly = 1) { userReporter.reportUserId(sessionToken.userId) }
        verify(exactly = 1) { trust.setUserId(sessionToken.userId) }
    }
}
