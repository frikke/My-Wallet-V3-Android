package com.blockchain.coincore.erc20

import com.blockchain.annotations.CommonCode
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isErc20
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import thepit.PitLinking

internal class Erc20Asset(
    override val assetInfo: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    features: InternalFeatureFlagApi,
    private val availableCustodialActions: Set<AssetAction>,
    private val availableNonCustodialActions: Set<AssetAction>,
    private val formatUtils: FormatUtilities
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
) {
    private val erc20address
        get() = erc20DataManager.accountHash

    override val isCustodialOnly: Boolean = assetInfo.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            if (assetInfo.categories.contains(AssetCategory.NON_CUSTODIAL)) {
                listOf(getNonCustodialAccount())
            } else {
                emptyList()
            }
        }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        if (assetInfo.categories.contains(AssetCategory.CUSTODIAL)) {
            Single.just(
                listOf(
                    CustodialTradingAccount(
                        currency = assetInfo,
                        label = labels.getDefaultCustodialWalletLabel(),
                        exchangeRates = exchangeRates,
                        custodialWalletManager = custodialManager,
                        tradingBalances = tradingBalances,
                        identity = identity,
                        features = features,
                        baseActions = availableCustodialActions
                    )
                )
            )
        } else {
            Single.just(emptyList())
        }

    private fun getNonCustodialAccount(): Erc20NonCustodialAccount =
        Erc20NonCustodialAccount(
            payloadManager,
            assetInfo,
            erc20DataManager,
            erc20address,
            feeDataManager,
            labels.getDefaultNonCustodialWalletLabel(),
            exchangeRates,
            walletPreferences,
            custodialManager,
            availableNonCustodialActions,
            identity
        )

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address))
            .flatMapMaybe { isValid ->
                if (isValid) {
                    erc20DataManager.isContractAddress(address)
                        .flatMapMaybe { isContract ->
                            Maybe.just(
                                Erc20Address(
                                    asset = assetInfo,
                                    address = address,
                                    label = label ?: address,
                                    isContract = isContract
                                )
                            )
                        }
                } else {
                    Maybe.empty()
                }
            }

    override fun isValidAddress(address: String): Boolean =
        formatUtils.isValidEthereumAddress(address)
}

internal open class Erc20Address(
    final override val asset: AssetInfo,
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    val isContract: Boolean = false
) : CryptoAddress {
    init {
        require(asset.isErc20())
    }
}
