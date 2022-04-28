package com.blockchain.domain.eligibility

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import io.reactivex.rxjava3.core.Single

interface EligibilityService {
    fun getCustodialEligibleCountries(): Single<List<CountryIso>>

    fun getProductEligibility(product: EligibleProduct): Single<ProductEligibility>
}
