package com.blockchain.blockchaincard.data

import com.blockchain.api.bccardapi.models.ProductsResponse
import com.blockchain.nabu.Authenticator
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

class BcCardDataRepository(
    val bcCardService: BcCardService,
    private val authenticator: Authenticator
) {

    fun getProducts(): Single<List<BcCardProduct>> =
        authenticator.authenticate { tokenResponse ->
            bcCardService.getProducts(
                tokenResponse.authHeader
            ).map { response ->
                response.map {
                    it.toDomainModel()
                }
            }
        }
}

private fun ProductsResponse.toDomainModel(): BcCardProduct =
    BcCardProduct(
        productCode = productCode,
        price = FiatValue.fromMajor(
            fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
            major = BigDecimal(price.value)
        ),
        brand = BcCardBrand.valueOf(brand),
        type = BcCardType.valueOf(type)
    )

data class BcCardProduct(
    val productCode: String,
    val price: FiatValue,
    val brand: BcCardBrand,
    val type: BcCardType
)

enum class BcCardBrand {
    VISA,
    MASTERCARD,
    UNKNOWN // TODO should we have this safe guard??
}

enum class BcCardType {
    VIRTUAL,
    PHYSICAL,
    UNKNOWN // TODO should we have this safe guard??
}
