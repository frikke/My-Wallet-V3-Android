package com.blockchain.coincore.eth

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.NotificationAddresses
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.then
import com.blockchain.utils.thenSingle
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

internal class EthAsset(
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    private val assetCatalogue: Lazy<AssetCatalogue>,
    private val walletPrefs: WalletStatusPrefs,
    private val notificationUpdater: BackendNotificationUpdater,
    private val formatUtils: FormatUtilities,
    private val labels: DefaultLabels,
    private val addressResolver: EthHotWalletAddressResolver
) : CryptoAssetBase(),
    NonCustodialSupport {
    override val currency: AssetInfo
        get() = CryptoCurrency.ETHER

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            labels.getDefaultNonCustodialWalletLabel()
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> {

        val renamedAccount = ethDataManager.ehtAccount.takeIf { it.labelNeedsUpdate() }?.let {
            ethDataManager.updateAccountLabel(it.label.updatedLabel()).onErrorComplete()
        } ?: Completable.complete()

        return renamedAccount.then {
            updateBackendNotificationAddresses(ethDataManager.ehtAccount.address)
        }.thenSingle {
            Single.just(
                listOf(
                    EthCryptoWalletAccount(
                        ethDataManager = ethDataManager,
                        fees = feeDataManager,
                        jsonAccount = ethDataManager.ehtAccount,
                        walletPreferences = walletPrefs,
                        exchangeRates = exchangeRates,
                        assetCatalogue = assetCatalogue.value,
                        addressResolver = addressResolver,
                    )
                )
            )
        }
    }

    private fun updateBackendNotificationAddresses(address: String): Completable {
        val notify = NotificationAddresses(
            assetTicker = currency.networkTicker,
            addressList = listOf(address)
        )
        return notificationUpdater.updateNotificationBackend(notify)
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> {
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
                    isDomain = isDomainAddress,
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
    override val isDomain: Boolean = false,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null,
    val isContract: Boolean = false
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.ETHER
}
