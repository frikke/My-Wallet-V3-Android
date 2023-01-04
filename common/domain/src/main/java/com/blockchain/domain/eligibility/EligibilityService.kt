package com.blockchain.domain.eligibility

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface EligibilityService {
    suspend fun getCountriesList(scope: GetRegionScope): Outcome<Exception, List<Region.Country>>

    suspend fun getStatesList(
        countryCode: CountryIso,
        scope: GetRegionScope
    ): Outcome<Exception, List<Region.State>>

    @Deprecated("use flow getProductEligibility")
    suspend fun getProductEligibilityLegacy(product: EligibleProduct): Outcome<Exception, ProductEligibility>

    fun getProductEligibility(
        product: EligibleProduct,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<ProductEligibility>>

    suspend fun getMajorProductsNotEligibleReasons(): Outcome<Exception, List<ProductNotEligibleReason>>
}
