package info.blockchain.wallet.payload

import com.blockchain.api.bitcoin.data.BalanceResponseDto
import com.blockchain.api.services.NonCustodialBitcoinService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import java.math.BigInteger
import retrofit2.Call

abstract class BalanceManager constructor(
    val bitcoinApi: NonCustodialBitcoinService,
    private val asset: AssetInfo
) {
    private var balanceMap: CryptoBalanceMap

    val walletBalance: BigInteger
        get() = balanceMap.totalSpendable.toBigInteger()

    val importedAddressesBalance: BigInteger
        get() = balanceMap.totalSpendableImported.toBigInteger()

    private val balanceQuery: BalanceCall
        get() = BalanceCall(bitcoinApi, asset)

    init {
        balanceMap = CryptoBalanceMap.zero(asset)
    }

    fun subtractAmountFromAddressBalance(address: String, amount: BigInteger) {
        balanceMap = balanceMap.subtractAmountFromAddress(address, CryptoValue(asset, amount))
    }

    fun getAddressBalance(xpub: XPubs): CryptoValue =
        (
            getXpubBalance(xpub.forDerivation(XPub.Format.LEGACY)) +
                getXpubBalance(xpub.forDerivation(XPub.Format.SEGWIT))
            ) as CryptoValue

    private fun getXpubBalance(xpub: XPub?): CryptoValue =
        xpub?.address?.let {
            return balanceMap[it]
        } ?: CryptoValue.zero(asset)

    fun updateAllBalances(
        xpubs: List<XPubs>,
        importedAddresses: List<String>
    ) {
        balanceMap = calculateCryptoBalanceMap(
            asset = asset,
            balanceQuery = balanceQuery,
            xpubs = xpubs,
            imported = importedAddresses
        )
    }

    fun resetAllBalances() {
        balanceMap = CryptoBalanceMap.zero(asset)
    }

    @Deprecated("Use getBalanceQuery")
    abstract fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto>
}
