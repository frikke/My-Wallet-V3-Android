package info.blockchain.wallet.multiaddress

import com.blockchain.api.bitcoin.data.MultiAddress
import com.blockchain.api.services.NonCustodialBitcoinService
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import retrofit2.Call

class MultiAddressFactoryBch(bitcoinApi: NonCustodialBitcoinService) : MultiAddressFactory(bitcoinApi) {

    override fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        onlyShow: List<String>?
    ): Call<MultiAddress> =
        bitcoinApi.getMultiAddress(
            NonCustodialBitcoinService.BITCOIN_CASH,
            xpubs.legacyXpubAddresses(),
            emptyList(),
            onlyShow?.joinToString("|"),
            NonCustodialBitcoinService.BalanceFilter.DoNotFilter,
            limit,
            offset
        )
}
