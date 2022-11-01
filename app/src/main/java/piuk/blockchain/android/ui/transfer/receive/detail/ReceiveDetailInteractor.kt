package piuk.blockchain.android.ui.transfer.receive.detail

import com.blockchain.coincore.loader.UniversalDynamicAssetRepository
import com.blockchain.core.chains.EvmNetwork
import io.reactivex.rxjava3.core.Maybe

class ReceiveDetailInteractor(
    val dynamicAssetRepository: UniversalDynamicAssetRepository
) {
    fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork> {
        return dynamicAssetRepository.getEvmNetworkForCurrency(currency)
    }
}
