package piuk.blockchain.android.ui.interest.tbm.data.repository

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.service.TierService
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitFirst
import kotlinx.coroutines.rx3.awaitSingle
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestService
import timber.log.Timber

class AssetInterestRepository(
    private val kycTierService: TierService,
    private val interestBalance: InterestBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val coincore: Coincore,
    private val dispatcher: CoroutineDispatcher
) : AssetInterestService {

    override suspend fun getInterestDetail(): Outcome<Throwable, InterestDetail> {
        return coroutineScope {
            val deferredTiers = async(dispatcher) { kycTierService.tiers().await() }
            val deferredEnabledAssets = async(dispatcher) { custodialWalletManager.getInterestEnabledAssets().await() }

            try {
                val tiers = deferredTiers.await()
                val enabledAssets = deferredEnabledAssets.await()

                Outcome.Success(InterestDetail(tiers = tiers, enabledAssets = enabledAssets))
            } catch (e: Throwable) {
                Outcome.Failure(e)
            }
        }
    }

    override suspend fun getAssetsInterestInfo(
        cryptoCurrencies: List<AssetInfo>
    ): Outcome<Throwable, List<AssetInterestInfo>> {
        return coroutineScope {
            cryptoCurrencies.map { asset -> async(dispatcher) { getAssetInterestInfo(asset) } }
                .map { deferred -> deferred.await() }
                .run { Outcome.Success(this) }
        }
    }

    private suspend fun getAssetInterestInfo(cryptoCurrency: AssetInfo): AssetInterestInfo {
        return coroutineScope {
            val deferredBalance =
                async(dispatcher) { interestBalance.getBalanceForAsset(cryptoCurrency).awaitFirst() }
            val deferredExchangeRate =
                async(dispatcher) { exchangeRatesDataManager.exchangeRateToUserFiat(cryptoCurrency).awaitFirst() }
            val deferredInterestRate =
                async(dispatcher) { custodialWalletManager.getInterestAccountRates(cryptoCurrency).await() }
            val deferredEligibility =
                async(dispatcher) { custodialWalletManager.getInterestEligibilityForAsset(cryptoCurrency).await() }

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
                Timber.e("Error loading interest dashboard item: $cryptoCurrency")
                null
            }

            AssetInterestInfo(
                assetInfo = cryptoCurrency,
                assetInterestDetail = assetInterestDetail
            )
        }
    }

    override suspend fun getAccountGroup(
        cryptoCurrency: AssetInfo,
        filter: AssetFilter
    ): Outcome<Throwable, AccountGroup> {
        return try {
            coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).awaitSingle()
                .run { Outcome.Success(this) }
        } catch (e: Throwable) {
            Outcome.Failure(e)
        }
    }
}