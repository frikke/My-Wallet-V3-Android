package com.blockchain.nabu.datamanagers

import com.blockchain.api.ApiException
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.nabu.metadata.NabuLegacyCredentialsMetadata
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.toNabuOfflineToken
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.SessionPrefs
import com.blockchain.utils.Optional
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asObservable

interface NabuDataManager {

    suspend fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String
    ): Outcome<Exception, Unit>

    suspend fun isProfileNameValid(firstName: String, lastName: String): Outcome<Exception, Boolean>

    fun requestJwt(): Single<String>

    fun getAirdropCampaignStatus(): Single<AirdropStatusList>

    fun addAddress(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable

    fun recordCountrySelection(
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable

    fun startVeriffSession(): Single<VeriffApplicantAndToken>

    fun submitVeriffVerification(): Completable

    fun getSupportedDocuments(
        countryCode: String
    ): Single<List<SupportedDocuments>>

    fun registerCampaign(
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable

    fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse>

    fun clearAccessToken()

    fun refreshToken(offlineToken: NabuOfflineToken): Single<NabuSessionTokenResponse>

    fun currentToken(offlineToken: NabuOfflineToken): Single<NabuSessionTokenResponse>

    fun recoverLegacyAccount(userId: String, recoveryToken: String): Single<NabuLegacyCredentialsMetadata>

    fun recoverBlockchainAccount(userId: String, recoveryToken: String): Single<BlockchainAccountCredentialsMetadata>

    fun resetUserKyc(): Completable
}

internal class NabuDataManagerImpl(
    private val nabuService: NabuService,
    private val retailWalletTokenService: RetailWalletTokenService,
    private val nabuTokenStore: NabuSessionTokenStore,
    private val appVersion: String,
    private val settingsDataManager: SettingsDataManager,
    private val userReporter: NabuUserReporter,
    private val walletReporter: WalletReporter,
    private val trust: DigitalTrust,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: SessionPrefs,
    private val userService: UserService
) : NabuDataManager {

    private val guid
        get() = payloadDataManager.guid
    private val sharedKey
        get() = payloadDataManager.sharedKey
    private val emailSingle
        get() = settingsDataManager.getSettings()
            .doOnNext { walletReporter.reportUserSettings(it) }
            .map { it.email }
            .singleOrError()

    override fun requestJwt(): Single<String> =
        retailWalletTokenService.requestJwt(
            guid = guid,
            sharedKey = sharedKey
        ).map {
            if (it.isSuccessful) {
                return@map it.token!!
            } else {
                throw ApiException(it.error)
            }
        }

    override fun getAuthToken(
        jwt: String
    ): Single<NabuOfflineTokenResponse> =
        nabuService.getAuthToken(jwt).doOnSuccess {
            userReporter.reportUserId(it.userId)
            trust.setUserId(it.userId)
        }

    private var sessionToken: Single<NabuSessionTokenResponse>? = null

    // VisibleForTesting
    @Synchronized
    fun getSessionToken(
        offlineTokenResponse: NabuOfflineToken
    ): Single<NabuSessionTokenResponse> {
        sessionToken?.let {
            return it
        } ?: kotlin.run {
            return getSessionTokenCachedRequest(offlineTokenResponse)
                .doFinally {
                    sessionToken = null
                }
                .also {
                    sessionToken = it
                }
        }
    }

    private fun getSessionTokenCachedRequest(
        offlineTokenResponse: NabuOfflineToken
    ): Single<NabuSessionTokenResponse> {
        return emailSingle.flatMap {
            nabuService.getSessionToken(
                userId = offlineTokenResponse.userId,
                offlineToken = offlineTokenResponse.token,
                guid = guid,
                email = it,
                appVersion = appVersion,
                deviceId = prefs.deviceId
            )
        }.cache()
    }

    override suspend fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String
    ): Outcome<Exception, Unit> =
        nabuService.createBasicUser(
            firstName,
            lastName,
            dateOfBirth
        )

    override suspend fun isProfileNameValid(firstName: String, lastName: String): Outcome<Exception, Boolean> =
        nabuService.isProfileNameValid(firstName, lastName)

    override fun getAirdropCampaignStatus(): Single<AirdropStatusList> =
        nabuService.getAirdropCampaignStatus()

    override fun addAddress(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable =
        nabuService.addAddress(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        )

    override fun recordCountrySelection(
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable =
        nabuService.recordCountrySelection(
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        )

    override fun startVeriffSession(): Single<VeriffApplicantAndToken> =
        nabuService.startVeriffSession()

    // TODO(aromano): move these to KycService once Othman's PR is merged and remove NabuDataManager dependency on UserService
    override fun submitVeriffVerification(): Completable =
        userService.getUserFlow(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .asObservable()
            .firstOrError()
            .flatMapCompletable { user ->
                nabuService.submitVeriffVerification(user.id)
            }

    override fun registerCampaign(
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable =
        nabuService.registerCampaign(campaignRequest, campaignName)

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    override fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    override fun getSupportedDocuments(
        countryCode: String
    ): Single<List<SupportedDocuments>> =
        nabuService.getSupportedDocuments(countryCode)

    // TODO: Replace prefix checking with a proper error code -> needs backend changes
    private fun userRestored(throwable: Throwable): Boolean =
        (throwable as? NabuApiException?)?.let { nabuApiException ->
            nabuApiException.getErrorStatusCode() == NabuErrorStatusCodes.Conflict &&
                !nabuApiException.isUserWalletLinkError()
        } ?: false

    override fun currentToken(offlineToken: NabuOfflineToken): Single<NabuSessionTokenResponse> =
        if (nabuTokenStore.requiresRefresh()) {
            refreshToken(offlineToken)
        } else {
            nabuTokenStore.getAccessToken()
                .map { (it as Optional.Some).element }
                .singleOrError()
        }

    override fun recoverLegacyAccount(userId: String, recoveryToken: String): Single<NabuLegacyCredentialsMetadata> {
        return requestJwt().flatMap { jwt ->
            nabuService.recoverAccount(
                userId = userId,
                jwt = jwt,
                recoveryToken = recoveryToken
            )
                .map { recoverAccountResponse ->
                    NabuLegacyCredentialsMetadata(
                        userId = userId,
                        lifetimeToken = recoverAccountResponse.token
                    )
                }
        }
    }

    override fun recoverBlockchainAccount(
        userId: String,
        recoveryToken: String
    ): Single<BlockchainAccountCredentialsMetadata> {
        return requestJwt().flatMap { jwt ->
            nabuService.recoverAccount(
                userId = userId,
                jwt = jwt,
                recoveryToken = recoveryToken
            ).map { recoverAccountResponse ->
                BlockchainAccountCredentialsMetadata(
                    userId = userId,
                    lifetimeToken = recoverAccountResponse.token,
                    exchangeUserId = recoverAccountResponse.userCredentialsId,
                    exchangeLifetimeToken = recoverAccountResponse.mercuryLifetimeToken
                )
            }
        }
    }

    override fun resetUserKyc(): Completable {
        return requestJwt().flatMapCompletable { jwt ->
            nabuService.getAuthToken(jwt).flatMapCompletable { response ->
                nabuService.resetUserKyc(response.toNabuOfflineToken(), jwt)
            }
        }
    }

    private fun recoverOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineToken
    ): SingleSource<NabuSessionTokenResponse> =
        if (userRestored(throwable)) {
            recoverUserAndContinue(offlineToken)
        } else {
            Single.error(throwable as Exception)
        }

    private fun recoverUserAndContinue(
        offlineToken: NabuOfflineToken
    ): Single<NabuSessionTokenResponse> =
        requestJwt()
            .flatMapCompletable { nabuService.recoverUser(offlineToken, it) }
            .andThen(refreshToken(offlineToken))

    override fun refreshToken(
        offlineToken: NabuOfflineToken
    ): Single<NabuSessionTokenResponse> =
        getSessionToken(offlineToken)
            .subscribeOn(Schedulers.io())
            .flatMapObservable(nabuTokenStore::store)
            .singleOrError()
            .onErrorResumeNext {
                recoverOrReturnError(it, offlineToken)
            }
}
