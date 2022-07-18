package com.blockchain.nabu.service

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.kyc.domain.KycStoreService
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.TierUpdateJson
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

internal class NabuTierService(
    private val endpoint: Nabu,
    private val kycStoreService: KycStoreService,
    private val authenticator: Authenticator
) : TierService, TierUpdater {

    override fun tiers(): Single<KycTiers> =
        kycStoreService.getKycTiers()
            .subscribeOn(Schedulers.io())

    override fun setUserTier(tier: Int): Completable =
        authenticator.authenticate {
            endpoint.setTier(
                TierUpdateJson(tier),
                it.authHeader
            ).toSingleDefault(tier)
        }.ignoreElement()
}
