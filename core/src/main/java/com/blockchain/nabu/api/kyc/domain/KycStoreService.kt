package com.blockchain.nabu.api.kyc.domain

import com.blockchain.nabu.models.responses.nabu.KycTiers
import io.reactivex.rxjava3.core.Single

interface KycStoreService {
    fun getKycTiers(): Single<KycTiers>
}
