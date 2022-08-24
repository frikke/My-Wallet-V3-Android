package com.blockchain.nabu.api.getuser.data

import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.sdd.domain.SddService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.asObservable
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService{
    override fun getUser(): Single<NabuUser> =
        getUserStore
            .stream(FreshnessStrategy.Cached(forceRefresh = false))
            .asObservable()
            .firstElement()
            .toSingle()

    override fun getUserFlow(refreshStrategy: FreshnessStrategy): Flow<NabuUser> =
        getUserStore.stream(refreshStrategy)
            .getDataOrThrow()
}
