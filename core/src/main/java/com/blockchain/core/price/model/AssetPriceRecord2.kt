package com.blockchain.core.price.model

import com.blockchain.domain.common.model.Millis
import java.math.BigDecimal

data class AssetPriceRecord2(
    val base: String,
    val quote: String,
    val rate: BigDecimal? = null,
    val fetchedAt: Millis,
    val marketCap: Double? = null,
)
