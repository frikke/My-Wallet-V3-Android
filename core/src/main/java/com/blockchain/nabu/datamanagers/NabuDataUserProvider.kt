package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single

interface NabuDataUserProvider {
    fun getUser(): Single<NabuUser>
}

internal class NabuDataUserProviderNabuDataManagerAdapter(
    private val getUserStoreService: GetUserStoreService
) : NabuDataUserProvider {

    // todo(othman) NabuDataUserProviderNabuDataManagerAdapter will be replaced by the store directly
    // not doing here because the pr is gonna be too big
    override fun getUser(): Single<NabuUser> {
        return getUserStoreService.getUser()
    }
}
