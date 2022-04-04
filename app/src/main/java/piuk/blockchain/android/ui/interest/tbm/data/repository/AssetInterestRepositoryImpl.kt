package piuk.blockchain.android.ui.interest.tbm.data.repository

import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitFirst
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository
import timber.log.Timber

class AssetInterestRepositoryImpl(
    private val kycTierService: TierService,
    private val interestBalance: InterestBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val dispatcher: CoroutineDispatcher
) : AssetInterestRepository {

    override suspend fun getInterestDetail(): Result<InterestDetail> {
        return coroutineScope {
            val deferredTiers = async { kycTierService.tiers().await() }
            val deferredEnabledAssets = async { custodialWalletManager.getInterestEnabledAssets().await() }

            try {
                val tiers = deferredTiers.await()
                val enabledAssets = deferredEnabledAssets.await()

                println("------ $tiers")
                println("------ $enabledAssets")

                Result.success(InterestDetail(tiers = tiers, enabledAssets = enabledAssets))
            } catch (e: Throwable) {
                Result.failure(e)
            }

        }
    }

    override suspend fun getAssetsInterestInfo(assets: List<AssetInfo>): Result<List<AssetInterestInfo>> {
        return coroutineScope {
            assets.map { asset -> async(dispatcher) { getAssetInterestInfo(asset) } }
                .map { deferred -> deferred.await() }
                .run { Result.success(this) }
        }
    }

    private suspend fun getAssetInterestInfo(assetInfo: AssetInfo): AssetInterestInfo {
        return coroutineScope {
            val deferredBalance = async(dispatcher) { interestBalance.getBalanceForAsset(asset).awaitFirst() }
            val deferredExchangeRate =
                async(dispatcher) { exchangeRatesDataManager.exchangeRateToUserFiat(asset).awaitFirst() }
            val deferredInterestRate =
                async(dispatcher) { custodialWalletManager.getInterestAccountRates(asset).await() }
            val deferredEligibility =
                async(dispatcher) { custodialWalletManager.getInterestEligibilityForAsset(asset).await() }

            val assetInterestDetail: AssetInterestDetail? = try {
                val balance = deferredBalance.await()
                val exchangeRate = deferredExchangeRate.await()
                val interestRate = deferredInterestRate.await()
                val eligibility = deferredEligibility.await()

                AssetInterestDetail(
                    totalInterest = balance.totalInterest,
                    totalBalance = balance.totalBalance,
                    rate = interestRate,
                    eligible = eligibility.eligible,
                    ineligibilityReason = eligibility.ineligibilityReason,
                    totalBalanceFiat = exchangeRate.convert(balance.totalBalance)
                )
            } catch (e: Throwable) {
                Timber.e("Error loading interest dashboard item: $assetInfo")
                null
            }

            AssetInterestInfo(
                assetInfo = assetInfo,
                assetInterestDetail = assetInterestDetail
            )
        }
    }
}