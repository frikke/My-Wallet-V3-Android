package piuk.blockchain.androidcore.data.bccard.impl

import com.blockchain.api.bccardapi.models.ProductsResponse
import com.blockchain.api.services.BcCardService
import com.blockchain.nabu.Authenticator
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.bccard.BcCardBrand
import piuk.blockchain.androidcore.data.bccard.BcCardDataManager
import piuk.blockchain.androidcore.data.bccard.BcCardProduct
import piuk.blockchain.androidcore.data.bccard.BcCardType

class BcCardDataManagerImpl(
    val bcCardService: BcCardService,
    private val authenticator: Authenticator
) : BcCardDataManager {

    override fun getProducts(): Single<List<BcCardProduct>> =
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
        fee = fee,
        brand = BcCardBrand.valueOf(brand),
        type = BcCardType.valueOf(type)
    )