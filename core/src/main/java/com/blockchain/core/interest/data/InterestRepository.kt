package com.blockchain.core.interest.data

import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.core.TransactionsCache
import com.blockchain.core.TransactionsRequest
import com.blockchain.core.interest.data.datasources.InterestAvailableAssetsTimedCache
import com.blockchain.core.interest.data.datasources.InterestBalancesStore
import com.blockchain.core.interest.data.datasources.InterestEligibilityTimedCache
import com.blockchain.core.interest.data.datasources.InterestLimitsTimedCache
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestActivity
import com.blockchain.core.interest.domain.model.InterestActivityAttributes
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.core.interest.domain.model.InterestState
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.interest.InterestWithdrawalBody
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import java.util.Date

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestBalancesStore: InterestBalancesStore,
    private val interestEligibilityTimedCache: InterestEligibilityTimedCache,
    private val interestAvailableAssetsTimedCache: InterestAvailableAssetsTimedCache,
    private val interestLimitsTimedCache: InterestLimitsTimedCache,
    private val authenticator: Authenticator,
    private val nabuService: NabuService,
    private val interestApiService: InterestApiService,
    private val transactionsCache: TransactionsCache,
) : InterestService {

    // balances
    private fun getBalancesFlow(refreshStrategy: FreshnessStrategy): Flow<Map<AssetInfo, InterestAccountBalance>> {
        return interestBalancesStore.stream(refreshStrategy)
            .mapData { mapAssetTickerWithBalance ->
                mapAssetTickerWithBalance.mapNotNull { (assetTicker, balanceDto) ->
                    (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                        asset to balanceDto.toInterestBalance(asset)
                    }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(refreshStrategy: FreshnessStrategy): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Observable<InterestAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> {
        return getBalancesFlow(refreshStrategy)
            .map { it.keys }
    }

    // availability
    override fun getAvailableAssetsForInterest(): Single<List<AssetInfo>> {
        return interestAvailableAssetsTimedCache.cached()
    }

    override fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean> {
        return getAvailableAssetsForInterest()
            .map { assets -> assets.contains(asset) }
            .onErrorResumeNext { Single.just(false) }
    }

    // eligibility
    override fun getEligibilityForAssets(): Single<Map<AssetInfo, InterestEligibility>> {
        return interestEligibilityTimedCache.cached()
    }

    override fun getEligibilityForAsset(asset: AssetInfo): Single<InterestEligibility> {
        return getEligibilityForAssets().map { mapAssetWithEligibility ->
            mapAssetWithEligibility[asset] ?: InterestEligibility.Ineligible.default()
        }
    }

    // limits
    override fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>> {
        return interestLimitsTimedCache.cached()
    }

    override fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits> {
        return getLimitsForAssets().map { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }
    }

    // rate
    override fun getInterestRate(asset: AssetInfo): Single<Double> {
        return authenticator.authenticate { sessionToken ->
            interestApiService.getInterestRates(sessionToken.authHeader, asset.networkTicker)
                .map { interestRateResponse -> interestRateResponse.rate }
                .defaultIfEmpty(0.0)
        }
    }

    // address
    override fun getAddress(asset: AssetInfo): Single<String> {
        return authenticator.authenticate { sessionToken ->
            nabuService.getInterestAddress(sessionToken, asset.networkTicker)
                .map { interestAddressResponse -> interestAddressResponse.address }
        }
    }

    // activity
    override fun getActivity(asset: AssetInfo): Single<List<InterestActivity>> {
        return transactionsCache.transactions(
            TransactionsRequest(product = PRODUCT_NAME, type = null)
        ).map { interestActivityResponse ->
            interestActivityResponse.items
                .filter { transaction ->
                    assetCatalogue.fromNetworkTicker(transaction.amount.symbol)?.networkTicker == asset.networkTicker
                }.map { transaction ->
                    transaction.toInterestActivity(asset)
                }
        }
    }

    // withdrawal
    override fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable {
        return authenticator.authenticateCompletable { sessionToken ->
            nabuService.createInterestWithdrawal(
                sessionToken = sessionToken,
                body = InterestWithdrawalBody(
                    withdrawalAddress = address,
                    amount = amount.toBigInteger().toString(),
                    currency = asset.networkTicker
                )
            )
        }
    }

    companion object {
        const val PRODUCT_NAME = "savings"
    }
}

// ///////////////
// EXTENSIONS
// ///////////////

private fun InterestAccountBalanceDto.toInterestBalance(asset: AssetInfo) =
    InterestAccountBalance(
        totalBalance = CryptoValue.fromMinor(asset, totalBalance.toBigInteger()),
        pendingInterest = CryptoValue.fromMinor(asset, pendingInterest.toBigInteger()),
        pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit.toBigInteger()),
        totalInterest = CryptoValue.fromMinor(asset, totalInterest.toBigInteger()),
        lockedBalance = CryptoValue.fromMinor(asset, lockedBalance.toBigInteger()),
        hasTransactions = true
    )

private fun zeroBalance(asset: Currency): InterestAccountBalance =
    InterestAccountBalance(
        totalBalance = Money.zero(asset),
        pendingInterest = Money.zero(asset),
        pendingDeposit = Money.zero(asset),
        totalInterest = Money.zero(asset),
        lockedBalance = Money.zero(asset)
    )

private fun TransactionResponse.toInterestActivity(asset: AssetInfo): InterestActivity =
    InterestActivity(
        value = CryptoValue.fromMinor(asset, amountMinor.toBigInteger()),
        id = id,
        insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        state = state.toInterestState(),
        type = type.toTransactionType(),
        extraAttributes = extraAttributes?.toDomain()
    )

private fun TransactionAttributesResponse.toDomain() = InterestActivityAttributes(
    address = address,
    confirmations = confirmations,
    hash = hash,
    id = id,
    transactionHash = txHash,
    transferType = transferType,
    beneficiary = beneficiary
)

private fun String.toInterestState(): InterestState =
    when (this) {
        TransactionResponse.FAILED -> InterestState.FAILED
        TransactionResponse.REJECTED -> InterestState.REJECTED
        TransactionResponse.PROCESSING -> InterestState.PROCESSING
        TransactionResponse.CREATED,
        TransactionResponse.COMPLETE -> InterestState.COMPLETE
        TransactionResponse.PENDING -> InterestState.PENDING
        TransactionResponse.MANUAL_REVIEW -> InterestState.MANUAL_REVIEW
        TransactionResponse.CLEARED -> InterestState.CLEARED
        TransactionResponse.REFUNDED -> InterestState.REFUNDED
        else -> InterestState.UNKNOWN
    }

fun String.toTransactionType() =
    when (this) {
        TransactionResponse.DEPOSIT -> TransactionSummary.TransactionType.DEPOSIT
        TransactionResponse.WITHDRAWAL -> TransactionSummary.TransactionType.WITHDRAW
        TransactionResponse.INTEREST_OUTGOING -> TransactionSummary.TransactionType.INTEREST_EARNED
        else -> TransactionSummary.TransactionType.UNKNOWN
    }