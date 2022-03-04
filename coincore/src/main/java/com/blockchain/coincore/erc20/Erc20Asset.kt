package com.blockchain.coincore.erc20

import com.blockchain.annotations.CommonCode
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
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
    private val availableCustodialActions: Set<AssetAction>,
    private val availableNonCustodialActions: Set<AssetAction>,
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
            identity,
            addressResolver
        )

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> {

        return if (address.startsWith(FormatUtilities.ETHEREUM_PREFIX)) {
            processEip681Format(address, label, isDomainAddress)
        } else {
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
                                        isDomain = isDomainAddress,
                                        isContract = isContract
                                    )
                                )
                            }
                    } else {
                        Maybe.empty()
                    }
                }
        }
    }

    override fun isValidAddress(address: String): Boolean =
        formatUtils.isValidEthereumAddress(address)

    private fun processEip681Format(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> {
        // Example: ethereum:contract_address@chain_id/transfer?address=receive_address&uint256=amount
        val normalisedAddress = address.removePrefix(FormatUtilities.ETHEREUM_PREFIX)
        val segments = normalisedAddress.split(FormatUtilities.ETHEREUM_ADDRESS_DELIMITER)
        val addressSegment = segments.getOrNull(0)
            ?.split(FormatUtilities.ETHEREUM_CHAIN_ID_DELIMITER) // Get rid of the optional chainId for now
            ?.getOrNull(0)
            ?.removeSuffix(ERC20_ADDRESS_METHOD_PART) // Remove the "/transfer" part if it's there
            ?: return Maybe.empty()

        // Return if the normalised address segment is invalid or the original address doesn't have the transfer
        // method param - in that case it's not an ERC20 payment request.
        if (!isValidAddress(addressSegment) || !address.contains(ERC20_ADDRESS_METHOD_PART)) return Maybe.empty()

        val params = if (segments.size > 1) {
            segments[1].split(FormatUtilities.ETHEREUM_PARAMS_DELIMITER)
        } else {
            emptyList()
        }
        val amountParam = params.find {
            it.startsWith(ERC20_ADDRESS_AMOUNT_PART, true)
        }?.let { param ->
            CryptoValue.fromMinor(
                assetInfo, param.removePrefix(ERC20_ADDRESS_AMOUNT_PART).toBigDecimal()
            )
        }

        val receiveAddress = params.find {
            it.startsWith(ERC20_ADDRESS_PART, true)
        }?.removePrefix(ERC20_ADDRESS_PART) ?: return Maybe.empty()

        return erc20DataManager.isContractAddress(addressSegment)
            .flatMapMaybe { isContract ->
                Maybe.just(
                    Erc20Address(
                        asset = assetInfo,
                        address = receiveAddress,
                        label = label ?: receiveAddress,
                        isDomain = isDomainAddress,
                        amount = amountParam,
                        isContract = isContract
                    )
                )
            }
    }

    companion object {
        private const val ERC20_ADDRESS_PART = "address="
        private const val ERC20_ADDRESS_AMOUNT_PART = "uint256="
        private const val ERC20_ADDRESS_METHOD_PART = "/transfer"
    }
}

internal class Erc20Address(
    override val asset: AssetInfo,
    override val address: String,
    override val label: String = address,
    override val isDomain: Boolean = false,
    override val amount: Money? = null,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    val isContract: Boolean = false
) : CryptoAddress {
    init {
        require(asset.isErc20())
    }
}
