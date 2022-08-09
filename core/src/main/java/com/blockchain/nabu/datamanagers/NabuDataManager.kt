package com.blockchain.nabu.datamanagers

import androidx.annotation.VisibleForTesting
import com.blockchain.api.ApiException
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.logging.DigitalTrust
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
import com.blockchain.utils.Optional
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeSource
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs

interface NabuDataManager {

    fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineToken
    ): Completable

    fun requestJwt(): Single<String>

    fun getAirdropCampaignStatus(
        offlineTokenResponse: NabuOfflineToken
    ): Single<AirdropStatusList>

    fun addAddress(
        offlineTokenResponse: NabuOfflineToken,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable

    fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineToken,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable

    fun startVeriffSession(
        offlineTokenResponse: NabuOfflineToken
    ): Single<VeriffApplicantAndToken>

    fun submitVeriffVerification(
        offlineTokenResponse: NabuOfflineToken
    ): Completable

    fun getSupportedDocuments(
        offlineTokenResponse: NabuOfflineToken,
        countryCode: String
    ): Single<List<SupportedDocuments>>

    fun registerCampaign(
        offlineTokenResponse: NabuOfflineToken,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable

    fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse>

    fun getSessionToken(offlineTokenResponse: NabuOfflineToken): Single<NabuSessionTokenResponse>

    fun <T> authenticate(
        offlineToken: NabuOfflineToken,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T>

    fun <T> authenticateMaybe(
        offlineToken: NabuOfflineToken,
        maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>
    ): Maybe<T>

    fun clearAccessToken()

    fun invalidateToken()

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
    private val prefs: SessionPrefs
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Synchronized
    override fun getSessionToken(
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
                userId = offlineTokenResponse.userId, // FLAG_AUTH_REMOVAL
                offlineToken = offlineTokenResponse.token,
                guid = guid,
                email = it,
                appVersion = appVersion,
                deviceId = prefs.deviceId
            )
        }.cache()
    }

    override fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineToken
    ): Completable =
        authenticate(offlineTokenResponse) {
            nabuService.createBasicUser(
                firstName,
                lastName,
                dateOfBirth,
                it
            ).toSingleDefault(Any())
        }.ignoreElement()

    override fun getAirdropCampaignStatus(
        offlineTokenResponse: NabuOfflineToken
    ): Single<AirdropStatusList> =
        authenticate(offlineTokenResponse) {
            nabuService.getAirdropCampaignStatus(it)
        }

    override fun addAddress(
        offlineTokenResponse: NabuOfflineToken,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.addAddress(
            it,
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ).toSingleDefault(Any())
    }.ignoreElement()

    override fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineToken,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.recordCountrySelection(
            it,
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        ).toSingleDefault(Any())
    }.ignoreElement()

    override fun startVeriffSession(
        offlineTokenResponse: NabuOfflineToken
    ): Single<VeriffApplicantAndToken> = authenticate(offlineTokenResponse) {
        nabuService.startVeriffSession(it)
    }

    override fun submitVeriffVerification(
        offlineTokenResponse: NabuOfflineToken
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.submitVeriffVerification(it)
            .toSingleDefault(Any())
    }.ignoreElement()

    override fun registerCampaign(
        offlineTokenResponse: NabuOfflineToken,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.registerCampaign(it, campaignRequest, campaignName)
            .toSingleDefault(Any())
    }.ignoreElement()

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    override fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    override fun getSupportedDocuments(
        offlineTokenResponse: NabuOfflineToken,
        countryCode: String
    ): Single<List<SupportedDocuments>> = authenticate(offlineTokenResponse) {
        nabuService.getSupportedDocuments(it, countryCode)
    }

    private fun unauthenticated(throwable: Throwable) =
        (throwable as? NabuApiException?)?.getErrorStatusCode() == NabuErrorStatusCodes.TokenExpired

    // TODO: Replace prefix checking with a proper error code -> needs backend changes
    private fun userRestored(throwable: Throwable): Boolean =
        (throwable as? NabuApiException?)?.let { nabuApiException ->
            nabuApiException.getErrorStatusCode() == NabuErrorStatusCodes.Conflict &&
                !nabuApiException.isUserWalletLinkError()
        } ?: false

    // TODO: Refactor this logic into a reusable, thoroughly tested class - see AND-1335
    override fun <T> authenticate(
        offlineToken: NabuOfflineToken,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> =
        currentToken(offlineToken)
            .flatMap { tokenResponse ->
                singleFunction(tokenResponse)
                    .onErrorResumeNext { refreshOrReturnError(it, offlineToken, singleFunction) }
            }

    override fun <T> authenticateMaybe(
        offlineToken: NabuOfflineToken,
        maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>
    ): Maybe<T> =
        currentToken(offlineToken)
            .flatMapMaybe { tokenResponse ->
                maybeFunction(tokenResponse)
                    .onErrorResumeNext { e: Throwable -> refreshOrReturnError(e, offlineToken, maybeFunction) }
            }

    override fun invalidateToken() {
        nabuTokenStore.invalidate()
    }

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

    private fun <T> refreshOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineToken,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): SingleSource<T> =
        if (unauthenticated(throwable)) {
            refreshToken(offlineToken)
                .doOnSubscribe { clearAccessToken() }
                .flatMap { singleFunction(it) }
        } else {
            Single.error(throwable)
        }

    private fun <T> refreshOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineToken,
        maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>
    ): MaybeSource<T> =
        if (unauthenticated(throwable)) {
            refreshToken(offlineToken)
                .doOnSubscribe { clearAccessToken() }
                .flatMapMaybe { maybeFunction(it) }
        } else {
            Maybe.error(throwable)
        }

    private fun recoverOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineToken
    ): SingleSource<NabuSessionTokenResponse> =
        if (userRestored(throwable)) {
            recoverUserAndContinue(offlineToken)
        } else {
            Single.error(throwable)
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
            .onErrorResumeNext { recoverOrReturnError(it, offlineToken) }
}
