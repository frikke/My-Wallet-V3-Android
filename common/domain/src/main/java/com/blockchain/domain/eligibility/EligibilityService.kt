package com.blockchain.domain.eligibility

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome

interface EligibilityService {
    suspend fun getCountriesList(scope: GetRegionScope): Outcome<EligibilityError, List<Region.Country>>

    suspend fun getStatesList(
        countryCode: CountryIso,
        scope: GetRegionScope
    ): Outcome<EligibilityError, List<Region.State>>

    suspend fun getProductEligibility(product: EligibleProduct): Outcome<EligibilityError, ProductEligibility>

    suspend fun getMajorProductsNotEligibleReasons(): Outcome<EligibilityError, List<ProductNotEligibleReason>>
}
