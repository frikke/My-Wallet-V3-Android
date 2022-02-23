package com.blockchain.coincore.eth

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.NotificationAddresses
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.isCustodialOnly
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import thepit.PitLinking

internal class EthAsset(
    payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    private val assetCatalogue: Lazy<AssetCatalogue>,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    private val walletPrefs: WalletStatus,
    private val notificationUpdater: BackendNotificationUpdater,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    private val formatUtils: FormatUtilities,
    addressResolver: EthHotWalletAddressResolver
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    currencyPrefs,
    labels,
    custodialManager,
    interestBalances,
    tradingBalances,
    pitLinking,
    crashLogger,
    identity,
    addressResolver
),
    NonCustodialSupport {
    override val assetInfo: AssetInfo
        get() = CryptoCurrency.ETHER

    override val isCustodialOnly: Boolean = assetInfo.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            assetCatalogue.value,
            labels.getDefaultNonCustodialWalletLabel()
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(ethDataManager.getEthWallet() ?: throw Exception("No ether wallet found"))
            .map { ethereumWallet ->
                ethereumWallet.account?.let { ethereumAccount ->
                    EthCryptoWalletAccount(
                        payloadManager = payloadManager,
                        ethDataManager = ethDataManager,
                        fees = feeDataManager,
                        jsonAccount = ethereumAccount,
                        walletPreferences = walletPrefs,
                        exchangeRates = exchangeRates,
                        custodialWalletManager = custodialManager,
                        identity = identity,
                        assetCatalogue = assetCatalogue.value,
                        addressResolver = addressResolver
                    )
                } ?: throw Exception("No ethereum account found")
            }.doOnSuccess {
                updateBackendNotificationAddresses(it)
            }.map {
                listOf(it)
            }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    currency = assetInfo,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    tradingBalances = tradingBalances,
                    identity = identity
                )
            )
        )

    private fun updateBackendNotificationAddresses(account: EthCryptoWalletAccount) {
        val notify = NotificationAddresses(
            assetTicker = assetInfo.networkTicker,
            addressList = listOf(account.address)
        )
        return notificationUpdater.updateNotificationBackend(notify)
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> {
        val normalisedAddress = address.removePrefix(FormatUtilities.ETHEREUM_PREFIX)
        val segments = normalisedAddress.split(FormatUtilities.ETHEREUM_ADDRESS_DELIMITER)
        val addressSegment = segments.getOrNull(0)
            ?.split(FormatUtilities.ETHEREUM_CHAIN_ID_DELIMITER)
            ?.getOrNull(0)
            ?: return Maybe.empty()

        if (!isValidAddress(addressSegment)) return Maybe.empty()

        val params = if (segments.size > 1) {
            segments[1].split(FormatUtilities.ETHEREUM_PARAMS_DELIMITER)
        } else {
            emptyList()
        }

        val amountParam = params.find {
            it.startsWith(ETH_ADDRESS_AMOUNT_PART, true)
        }?.let { param ->
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER, param.removePrefix(ETH_ADDRESS_AMOUNT_PART).toBigDecimal()
            )
        }

        return ethDataManager.isContractAddress(addressSegment)
            .map { isContract ->
                EthAddress(
                    address = addressSegment,
                    label = label ?: addressSegment,
                    amount = amountParam,
                    isContract = isContract
                ) as ReceiveAddress
            }.toMaybe()
    }

    override fun isValidAddress(address: String): Boolean =
        formatUtils.isValidEthereumAddress(address)

    companion object {
        private const val ETH_ADDRESS_AMOUNT_PART = "value="
    }
}

internal class EthAddress(
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null,
    val isContract: Boolean = false
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.ETHER
}
