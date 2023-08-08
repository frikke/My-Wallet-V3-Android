package com.blockchain.nabu.service.nabu

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.NabuBasicUser
import com.blockchain.nabu.models.responses.nabu.NabuJwt
import com.blockchain.nabu.models.responses.nabu.RecordCountryRequest
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.nabu.SupportedDocumentsResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.util.fakefactory.nabu.FakeAddressFactory
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuUserFactory
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class NabuServiceTest {

    private val nabu: Nabu = mock()
    private val remoteConfigPrefs: RemoteConfigPrefs = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val subject: NabuService = NabuService(
        nabu,
        remoteConfigPrefs,
        mock {
            on { tags(any()) }.thenReturn(emptyMap())
        },
        environmentConfig
    )

    private val jwt = "JWT"

    @Test
    fun getAuthToken() {
        val expectedTokenResponse = NabuOfflineTokenResponse(
            "d753109e-34c2-42bd-82f1-cc90470234kf",
            "d753109e-23jd-42bd-82f1-cc904702asdfkjf",
            true
        )

        whenever(
            nabu.getAuthToken(NabuOfflineTokenRequest(jwt))
        ).thenReturn(
            Single.just(
                expectedTokenResponse
            )
        )

        subject.getAuthToken(jwt).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.userId == expectedTokenResponse.userId
                it.token == expectedTokenResponse.token
            }
    }

    @Test
    fun getSessionToken() {
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val offlineToken = "OFFLINE_TOKEN"
        val appVersion = "6.14.0"
        val deviceId = "DEVICE_ID"

        val expectedSessionTokenResponse = FakeNabuSessionTokenFactory.any

        whenever(
            nabu.getSessionToken(userId, offlineToken, guid, email, appVersion, "APP", deviceId)
        ).thenReturn(
            Single.just(expectedSessionTokenResponse)
        )

        subject.getSessionToken(
            userId,
            offlineToken,
            guid,
            email,
            appVersion,
            deviceId
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedSessionTokenResponse
        }
    }

    @Test
    fun createBasicUser() = runTest {
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "12-12-1234"

        whenever(
            nabu.createBasicUser(NabuBasicUser(firstName, lastName, dateOfBirth))
        ).thenReturn(
            Outcome.Success(Unit)
        )

        val result = subject.createBasicUser(
            firstName,
            lastName,
            dateOfBirth
        )
        result shouldBeEqualTo Outcome.Success(Unit)
    }

    @Test
    fun getUser() {
        val expectedUser = FakeNabuUserFactory.satoshi

        whenever(
            nabu.getUser()
        ).thenReturn(
            Single.just(expectedUser)
        )

        subject.getUser().test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedUser
            }
    }

    @Test
    fun updateWalletInformation() {
        val expectedUser = FakeNabuUserFactory.satoshi

        whenever(
            nabu.updateWalletInformation(NabuJwt(jwt))
        ).thenReturn(
            Single.just(expectedUser)
        )
        subject.updateWalletInformation(jwt).test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedUser
            }
    }

    @Test
    fun addAddress() {
        val addressToAdd = FakeAddressFactory.any

        whenever(
            nabu.addAddress(AddAddressRequest(addressToAdd))
        ).thenReturn(
            Completable.complete()
        )

        subject.addAddress(
            addressToAdd.line1!!,
            addressToAdd.line2,
            addressToAdd.city!!,
            addressToAdd.stateIso,
            addressToAdd.postCode!!,
            addressToAdd.countryCode!!
        ).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun recordCountrySelection() {
        val countryCode = "US"
        val state = "US-AL"
        val notifyWhenAvailable = true

        whenever(
            nabu.recordSelectedCountry(
                RecordCountryRequest(jwt, countryCode, notifyWhenAvailable, state)
            )
        ).thenReturn(
            Completable.complete()
        )

        subject.recordCountrySelection(
            jwt,
            countryCode,
            state,
            notifyWhenAvailable
        ).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun getSupportedDocuments() {
        val countryCode = "US"
        val expectedDocuments = arrayListOf(SupportedDocuments.DRIVING_LICENCE, SupportedDocuments.PASSPORT)

        whenever(
            nabu.getSupportedDocuments(countryCode)
        ).thenReturn(
            Single.just(SupportedDocumentsResponse(countryCode, expectedDocuments))
        )

        subject.getSupportedDocuments(
            countryCode
        ).test().waitForCompletionWithoutErrors().assertValue {
            it == expectedDocuments
        }
    }

    @Test
    fun `recover user`() {
        val userId = "userID"
        val offlineToken = NabuOfflineToken(
            userId,
            "token"
        )

        whenever(
            nabu.recoverUser(userId, NabuJwt(jwt), "Bearer ${offlineToken.token}")
        ).thenReturn(
            Completable.complete()
        )

        subject.recoverUser(offlineToken, jwt).test().waitForCompletionWithoutErrors()
    }

    @Test
    fun `register for campaign`() {
        val campaignName = "name"
        val campaignRequest = RegisterCampaignRequest(
            mapOf("key" to "value"),
            true
        )

        whenever(
            nabu.registerCampaign(campaignRequest, campaignName)
        ).thenReturn(
            Completable.complete()
        )

        subject.registerCampaign(
            campaignRequest,
            campaignName
        ).test().waitForCompletionWithoutErrors()
    }
}
