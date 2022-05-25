package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import exchange.ExchangeLinking
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodialOnly
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class StxAsset(
    override val assetInfo: AssetInfo,
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    exchangeLinking: ExchangeLinking,
    remoteLogger: RemoteLogger,
    identity: UserIdentity,
    private val addressValidation: String? = null,
    private val availableActions: Set<AssetAction> = emptySet(),
    addressResolver: IdentityAddressResolver,
    private val stxForAllFeatureFlag: FeatureFlag,
    private val stxForAirdropFeatureFlag: FeatureFlag
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    currencyPrefs,
    labels,
    custodialManager,
    interestBalances,
    tradingBalances,
    exchangeLinking,
    remoteLogger,
    identity,
    addressResolver
) {
    override val isCustodialOnly: Boolean = assetInfo.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Singles.zip(
            stxForAllFeatureFlag.enabled,
            stxForAirdropFeatureFlag.enabled,
            identity.hasReceivedStxAirdrop()
        )
            .map { (isEnabledForAll, isEnabledForAirdropUsers, hasReceivedAirdrop) ->
                when {
                    // TODO(dtverdota): AND-6168 STX | Balances
                    isEnabledForAll -> emptyList()
                    isEnabledForAirdropUsers && hasReceivedAirdrop -> emptyList()
                    else -> emptyList()
                }
            }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(emptyList())

    private val addressRegex: Regex? by unsafeLazy {
        addressValidation?.toRegex()
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        addressRegex?.let {
            if (address.matches(it)) {
                Maybe.just(
                    DynamicNonCustodialAddress(
                        address = address,
                        asset = assetInfo as AssetInfo,
                        isDomain = isDomainAddress
                    )
                )
            } else {
                Maybe.empty()
            }
        } ?: Maybe.empty()
}
