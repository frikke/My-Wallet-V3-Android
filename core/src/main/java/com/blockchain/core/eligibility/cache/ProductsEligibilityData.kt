package com.blockchain.core.eligibility.cache

import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import kotlinx.serialization.Serializable

@Serializable
data class ProductsEligibilityData(
    val majorProductsNotEligibleReasons: List<ProductNotEligibleReason>,
    val products: Map<EligibleProduct, ProductEligibility>
)
