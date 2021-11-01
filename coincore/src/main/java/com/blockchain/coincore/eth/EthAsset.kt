package com.blockchain.coincore.eth

import com.blockchain.annotations.CommonCode
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isCustodialOnly
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import com.blockchain.coincore.AddressParseError
import com.blockchain.coincore.AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CryptoAssetBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.NotificationAddresses
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
    features: InternalFeatureFlagApi
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
    features
), NonCustodialSupport {
    override val asset: AssetInfo
        get() = CryptoCurrency.ETHER

    override val isCustodialOnly: Boolean = asset.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            assetCatalogue.value,
            labels.getDefaultNonCustodialWalletLabel()
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(ethDataManager.getEthWallet() ?: throw Exception("No ether wallet found"))
            .map {
                EthCryptoWalletAccount(
                    payloadManager = payloadManager,
                    ethDataManager = ethDataManager,
                    fees = feeDataManager,
                    jsonAccount = it.account,
                    walletPreferences = walletPrefs,
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    identity = identity,
                    assetCatalogue = assetCatalogue.value
                )
            }.doOnSuccess {
                updateBackendNotificationAddresses(it)
            }.map {
                listOf(it)
            }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    asset = asset,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    tradingBalances = tradingBalances,
                    identity = identity,
                    features = features
                )
            )
        )

    private fun updateBackendNotificationAddresses(account: EthCryptoWalletAccount) {
        val notify = NotificationAddresses(
            assetTicker = asset.networkTicker,
            addressList = listOf(account.address)
        )
        return notificationUpdater.updateNotificationBackend(notify)
    }

    @CommonCode("Exists in UsdtAsset and PaxAsset")
    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address)).flatMapMaybe { isValid ->
            if (isValid) {
                ethDataManager.isContractAddress(address).flatMapMaybe { isContract ->
                    if (isContract) {
                        throw AddressParseError(ETH_UNEXPECTED_CONTRACT_ADDRESS)
                    } else {
                        Maybe.just(EthAddress(address, label ?: address))
                    }
                }
            } else {
                Maybe.empty()
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class EthAddress(
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() }
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.ETHER
}
