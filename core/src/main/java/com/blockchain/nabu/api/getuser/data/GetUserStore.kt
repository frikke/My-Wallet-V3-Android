package com.blockchain.nabu.api.getuser.data

import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.logging.DigitalTrust
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
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import java.util.Calendar
import java.util.concurrent.TimeUnit
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class GetUserStore(
    private val nabuService: NabuService,
    private val userReporter: NabuUserReporter,
    private val trust: DigitalTrust,
    private val walletReporter: WalletReporter,
    private val sessionInfo: SessionInfo,
    private val payloadDataManager: PayloadDataManager
) : Store<NabuUser> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                nabuService.getUser()
                    .doOnSuccess { user ->
                        userReporter.reportUserId(user.id)
                        userReporter.reportUser(user)
                        trust.setUserId(user.id)
                        walletReporter.reportWalletGuid(payloadDataManager.guid)
                        sessionInfo.setUserId(user.id)
                    }
            }
        ),
        dataSerializer = NabuUser.serializer(),
        mediator = object : Mediator<Unit, NabuUser> {
            /**
             * cache would be considered expired in these caes:
             * * no kyc, expired, rejected, verified -> if cache is more than 1 hour
             * * pending, under review -> if cache is more than 3 minutes
             */
            fun shouldFetch(userState: UserState, kycState: KycState, dataAgeMillis: Long): Boolean {
                if (userState == UserState.None) return true
                // Phone clocked was changed
                if (dataAgeMillis < 0) return true

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
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "GetUserStore"
    }
}
