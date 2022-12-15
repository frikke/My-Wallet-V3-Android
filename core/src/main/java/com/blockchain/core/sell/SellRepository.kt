package com.blockchain.core.sell

import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellService
import com.blockchain.core.sell.domain.SellUserEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.utils.asFlow
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.asAssetInfoOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SellRepository(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val kycService: KycService,
    private val simpleBuyService: SimpleBuyService,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs
) : SellService {

    override fun loadSellAssets(freshnessStrategy: FreshnessStrategy): Flow<DataResource<SellEligibility>> =
        userFeaturePermissionService.getAccessForFeature(
            feature = Feature.Sell,
            freshnessStrategy = freshnessStrategy
        ).flatMapData { data ->
            checkUserEligibilityStatus(data)
        }

    private fun checkUserEligibilityStatus(access: FeatureAccess?): Flow<DataResource<SellEligibility>> =
        when (val reason = (access as? FeatureAccess.Blocked)?.reason) {
            is BlockedReason.InsufficientTier,
            is BlockedReason.NotEligible,
            is BlockedReason.Sanctions -> flowOf(DataResource.Data(SellEligibility.NotEligible(reason)))
            is BlockedReason.TooManyInFlightTransactions,
            is BlockedReason.ShouldAcknowledgeStakingWithdrawal,
            null -> {
                loadSellEligibility().flatMapData { data ->
                    when (data) {
                        SellUserEligibility.KycRejectedUser,
                        SellUserEligibility.NonKycdUser -> flowOf(
                            DataResource.Data(SellEligibility.KycBlocked(data)) as DataResource<SellEligibility>
                        )
                        SellUserEligibility.KycdUser -> {
                            getSellAssetList().mapData { list ->
                                SellEligibility.Eligible(list)
                            }
                        }
                    }
                }
            }
        }

    private fun loadSellEligibility(): Flow<DataResource<SellUserEligibility>> =
        combine(
            kycService.getTiers(FreshnessStrategy.Fresh),
            simpleBuyService.isEligible(FreshnessStrategy.Fresh)
        ) { kycTiers, isEligibleForBuy ->
            combineDataResources(kycTiers, isEligibleForBuy) { kyc, eligible ->
                when {
                    kyc.isApprovedFor(KycTier.GOLD) && eligible -> {
                        SellUserEligibility.KycdUser
                    }
                    kyc.isRejectedFor(KycTier.GOLD) -> {
                        SellUserEligibility.KycRejectedUser
                    }
                    kyc.isApprovedFor(KycTier.GOLD) && !eligible -> {
                        SellUserEligibility.KycRejectedUser
                    }
                    else -> {
                        SellUserEligibility.NonKycdUser
                    }
                }
            }
        }

    private fun getSellAssetList(): Flow<DataResource<List<AssetInfo>>> =
        combine(
            custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency),
            custodialWalletManager.getSupportedBuySellCryptoCurrencies().asFlow()
        ) { supportedFiats, supportedPairs ->
            supportedPairs
                .filter { supportedFiats.contains(it.destination) }
                .map { it.source.asAssetInfoOrThrow() }
        }.map {
            DataResource.Data(it) as DataResource<List<AssetInfo>>
        }.catch {
            emit(DataResource.Error(it as Exception))
        }
}
