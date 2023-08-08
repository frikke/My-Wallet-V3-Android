package com.blockchain.nabu.datamanagers

import com.blockchain.api.ApiException
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.wallet.RetailJwtResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.SessionPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class NabuDataManagerTest {

    private lateinit var subject: NabuDataManagerImpl
    private val nabuService: NabuService = mock()
    private val tokenService: RetailWalletTokenService = mock()
    private val nabuTokenStore: NabuSessionTokenStore = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val userReporter: NabuUserReporter = mock()
    private val walletReporter: WalletReporter = mock()
    private val digitalTrust: DigitalTrust = mock()
    private val prefs: SessionPrefs = mock()
    private val userService: UserService = mock()
    private val appVersion = "6.23.2"
    private val deviceId = "DEVICE_ID"
    private val email = "EMAIL"
    private val guid = "GUID"
    private val sharedKey = "SHARED_KEY"

    @Before
    fun setUp() {
        whenever(payloadDataManager.guid).thenReturn(guid)
        whenever(payloadDataManager.sharedKey).thenReturn(sharedKey)

        val settings: Settings = mock()
        whenever(settings.email).thenReturn(email)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        subject = NabuDataManagerImpl(
            nabuService,
            tokenService,
            nabuTokenStore,
            appVersion,
            settingsDataManager,
            userReporter,
            walletReporter,
            digitalTrust,
            payloadDataManager,
            prefs,
            userService
        )
    }

    @Test
    fun `createUser success`() {
        // Arrange
        val jwt = "JWT"
        whenever(
            tokenService.requestJwt(
                guid = guid,
                sharedKey = sharedKey
            )
        ).thenReturn(Single.just(RetailJwtResponse(true, jwt, null)))
        // Act
        val testObserver = subject.requestJwt().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(jwt)
        verify(tokenService).requestJwt(
            guid = guid,
            sharedKey = sharedKey
        )
    }

    @Test
    fun `createUser failure`() {
        // Arrange
        val error = "ERROR"
        whenever(
            tokenService.requestJwt(
                guid = guid,
                sharedKey = sharedKey
            )
        ).thenReturn(Single.just(RetailJwtResponse(false, null, error)))
        // Act
        val testObserver = subject.requestJwt().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(ApiException::class.java)
        verify(tokenService).requestJwt(
            guid = guid,
            sharedKey = sharedKey
        )
    }

    @Test
    fun getAuthToken() {
        // Arrange
        val userId = "USER_ID"
        val token = "TOKEN"
        val jwt = "JWT"
        val tokenResponse = NabuOfflineTokenResponse(userId, token, true)
        whenever(nabuService.getAuthToken(jwt))
            .thenReturn(Single.just(tokenResponse))
        // Act
        val testObserver = subject.getAuthToken(jwt).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(tokenResponse)
        verify(nabuService).getAuthToken(jwt)
    }

    @Test
    fun getSessionToken() {
        // Arrange
        val offlineToken = NabuOfflineToken("", "")
        val sessionTokenResponse = FakeNabuSessionTokenFactory.any
        whenever(
            nabuService.getSessionToken(
                userId = offlineToken.userId,
                offlineToken = offlineToken.token,
                guid = guid,
                email = email,
                deviceId = deviceId,
                appVersion = appVersion
            )
        ).thenReturn(Single.just(sessionTokenResponse))
        whenever(prefs.deviceId).thenReturn(deviceId)
        // Act
        val testObserver = subject.getSessionToken(offlineToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(sessionTokenResponse)
        verify(nabuService).getSessionToken(
            userId = offlineToken.userId,
            offlineToken = offlineToken.token,
            guid = guid,
            email = email,
            deviceId = deviceId,
            appVersion = appVersion
        )
    }

    @Test
    fun createBasicUser() = runTest {
        // Arrange
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "25-02-1995"
        whenever(
            nabuService.createBasicUser(
                firstName,
                lastName,
                dateOfBirth
            )
        ).thenReturn(Outcome.Success(Unit))
        // Act
        val result = subject.createBasicUser(
            firstName,
            lastName,
            dateOfBirth
        )
        // Assert
        result shouldBeEqualTo Outcome.Success(Unit)
        verify(nabuService).createBasicUser(
            firstName,
            lastName,
            dateOfBirth
        )
    }

    @Test
    fun addAddress() {
        // Arrange
        val city = "CITY"
        val line1 = "LINE1"
        val line2 = "LINE2"
        val state = null
        val countryCode = "COUNTRY_CODE"
        val postCode = "POST_CODE"
        whenever(
            nabuService.addAddress(
                line1,
                line2,
                city,
                state,
                postCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.addAddress(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).addAddress(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        )
    }

    @Test
    fun recordCountrySelection() {
        // Arrange
        val jwt = "JWT"
        val countryCode = "US"
        val stateCode = "US-AL"
        val notifyWhenAvailable = true
        whenever(
            nabuService.recordCountrySelection(
                jwt,
                countryCode,
                stateCode,
                notifyWhenAvailable
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.recordCountrySelection(
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).recordCountrySelection(
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        )
    }

    @Test
    fun getSupportedDocuments() {
        // Arrange
        val countryCode = "US"
        whenever(
            nabuService.getSupportedDocuments(
                countryCode
            )
        ).thenReturn(Single.just(listOf(SupportedDocuments.PASSPORT)))
        // Act
        val testObserver = subject.getSupportedDocuments(
            countryCode
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(listOf(SupportedDocuments.PASSPORT))
        verify(nabuService).getSupportedDocuments(
            countryCode
        )
    }

    @Test
    fun registerCampaign() {
        // Arrange
        val campaignRequest = RegisterCampaignRequest(emptyMap(), false)
        whenever(nabuService.registerCampaign(campaignRequest, "campaign"))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.registerCampaign(
            campaignRequest,
            "campaign"
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).registerCampaign(campaignRequest, "campaign")
    }

    companion object {
        private const val USER_ID = "1"
    }
}
