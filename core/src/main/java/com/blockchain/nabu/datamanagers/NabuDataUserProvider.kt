package com.blockchain.nabu.datamanagers

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.cache.UserCache
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

interface NabuDataUserProvider {
    fun getUser(): Single<NabuUser>
}

internal class NabuDataUserProviderNabuDataManagerAdapter(
    private val authenticator: NabuAuthenticator,
    private val userCache: UserCache,
    private val userReporter: NabuUserReporter,
    private val trust: DigitalTrust,
    private val walletReporter: WalletReporter,
    private val payloadDataManager: PayloadDataManager,
    private val getUserStoreService: GetUserStoreService,
    private val speedUpLoginUserFF: FeatureFlag
) : NabuDataUserProvider {

    override fun getUser(): Single<NabuUser> {
        return speedUpLoginUserFF.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                getUserStoreService.getUser()
            } else {
                authenticator.authenticate { tokenResponse ->
                    userCache.cached(tokenResponse)
                        .doOnSuccess {
                            userReporter.reportUserId(tokenResponse.userId)
                            userReporter.reportUser(it)
                            trust.setUserId(tokenResponse.userId)
                            walletReporter.reportWalletGuid(payloadDataManager.guid)
                        }
                }
            }
        }
    }
}
