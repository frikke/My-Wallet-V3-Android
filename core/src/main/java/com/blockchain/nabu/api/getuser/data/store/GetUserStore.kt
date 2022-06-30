package com.blockchain.nabu.api.getuser.data.store

import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.NabuUserReporter
import com.blockchain.nabu.datamanagers.WalletReporter
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal class GetUserStore(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val userReporter: NabuUserReporter,
    private val trust: DigitalTrust,
    private val walletReporter: WalletReporter,
    private val payloadDataManager: PayloadDataManager
) : Store<Throwable, NabuUser> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                authenticator.authenticate { tokenResponse ->
                    nabuService.getUser(tokenResponse)
                        .doOnSuccess {
                            userReporter.reportUserId(tokenResponse.userId)
                            userReporter.reportUser(it)
                            trust.setUserId(tokenResponse.userId)
                            walletReporter.reportWalletGuid(payloadDataManager.guid)
                        }

                }
            },
            errorMapper = { it }
        ),
        dataSerializer = NabuUser.serializer(),
        mediator = object : Mediator<Unit, NabuUser> {
            fun shouldFetch(userState: UserState, kycState: KycState, dataAgeMillis: Long): Boolean {
                if (userState == UserState.None) return true

                return when (kycState) {
                    KycState.Expired -> dataAgeMillis > TimeUnit.MINUTES.toMillis(60L)
                    KycState.None -> dataAgeMillis > TimeUnit.MINUTES.toMillis(60L)
                    KycState.Rejected -> dataAgeMillis > TimeUnit.MINUTES.toMillis(60L)
                    KycState.Verified -> dataAgeMillis > TimeUnit.MINUTES.toMillis(60L)
                    KycState.Pending -> dataAgeMillis > TimeUnit.MINUTES.toMillis(3L)
                    KycState.UnderReview -> dataAgeMillis > TimeUnit.MINUTES.toMillis(3L)
                }
            }

            override fun shouldFetch(cachedData: CachedData<Unit, NabuUser>?): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    userState = cachedData.data.state,
                    kycState = cachedData.data.kycState,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    GetUserDataSource {

    override fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, NabuUser>> =
        stream(StoreRequest.Cached(forceRefresh = refresh))

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "GetUserStore"
    }
}
