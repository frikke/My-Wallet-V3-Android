package piuk.blockchain.androidcore.data.bccard

import io.reactivex.rxjava3.core.Single

interface BcCardDataManager {
    fun getProducts() : Single<List<BcCardProduct>>
}

data class BcCardProduct(
    val productCode: String,
    val fee: Long,
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
    UNKNOWN
}