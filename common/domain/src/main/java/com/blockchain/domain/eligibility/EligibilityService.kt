package com.blockchain.domain.eligibility

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single

interface EligibilityService {
    fun getCustodialEligibleCountries(): Single<List<CountryIso>>

    suspend fun getProductEligibility(product: EligibleProduct): Outcome<EligibilityError, ProductEligibility>

    suspend fun getMajorProductsNotEligibleReasons(): Outcome<EligibilityError, List<ProductNotEligibleReason>>
}
