package com.blockchain.core.interest.data

import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestEligibilityDto
import com.blockchain.api.interest.data.InterestWithdrawalBodyDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.interest.data.datasources.InterestAvailableAssetsStore
import com.blockchain.core.interest.data.datasources.InterestBalancesStore
import com.blockchain.core.interest.data.datasources.InterestEligibilityStore
import com.blockchain.core.interest.data.datasources.InterestLimitsStore
import com.blockchain.core.interest.data.datasources.InterestRateStore
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestActivity
import com.blockchain.core.interest.domain.model.InterestActivityAttributes
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.core.interest.domain.model.InterestState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.asObservable
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
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestBalancesStore: InterestBalancesStore,
    private val interestEligibilityStore: InterestEligibilityStore,
    private val interestAvailableAssetsStore: InterestAvailableAssetsStore,
    private val interestLimitsStore: InterestLimitsStore,
    private val interestRateStore: InterestRateStore,
    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore,
    private val currencyPrefs: CurrencyPrefs,
    private val authenticator: Authenticator,
    private val interestApiService: InterestApiService
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
        return getAvailableAssetsForInterestFlow()
            .asObservable().firstOrError()
    }

    override fun getAvailableAssetsForInterestFlow(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<AssetInfo>>> {
        return interestAvailableAssetsStore.stream(refreshStrategy).mapData { response ->
            response.networkTickers.mapNotNull { networkTicker ->
                assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
            }
        }
    }

    override fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean> {
        return getAvailableAssetsForInterest()
            .map { assets -> assets.contains(asset) }
            .onErrorResumeNext { Single.just(false) }
    }

    override fun isAssetAvailableForInterestFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> {
        return getAvailableAssetsForInterestFlow(refreshStrategy)
            .mapData { assets -> assets.contains(asset) }
    }

    // eligibility
    override fun getEligibilityForAssets(): Single<Map<AssetInfo, InterestEligibility>> {
        return getEligibilityForAssetsFlow()
            .asObservable().firstOrError()
    }

    override fun getEligibilityForAssetsFlow(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, InterestEligibility>>> {
        fun String.toIneligibilityReason(): InterestEligibility.Ineligible {
            return when {
                this.isEmpty() -> InterestEligibility.Ineligible.NONE
                this == "NONE" -> InterestEligibility.Ineligible.NONE
                this == "UNSUPPORTED_REGION" -> InterestEligibility.Ineligible.REGION
                this == "INVALID_ADDRESS" -> InterestEligibility.Ineligible.REGION
                this == "TIER_TOO_LOW" -> InterestEligibility.Ineligible.KYC_TIER
                else -> InterestEligibility.Ineligible.OTHER
            }
        }

        return interestEligibilityStore.stream(refreshStrategy).mapData { mapAssetTicketWithEligibility ->
            assetCatalogue.supportedCustodialAssets.associateWith { asset ->
                val eligibilityDto = mapAssetTicketWithEligibility[asset.networkTicker.uppercase()]
                    ?: InterestEligibilityDto.default()

                if (eligibilityDto.isEligible) {
                    InterestEligibility.Eligible
                } else {
                    eligibilityDto.reason.toIneligibilityReason()
                }
            }
        }
    }

    override fun getEligibilityForAsset(asset: AssetInfo): Single<InterestEligibility> {
        return getEligibilityForAssets().map { mapAssetWithEligibility ->
            mapAssetWithEligibility[asset] ?: InterestEligibility.Ineligible.default()
        }
    }

    override fun getEligibilityForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<InterestEligibility>> {
        return getEligibilityForAssetsFlow(refreshStrategy)
            .mapData { mapAssetWithEligibility ->
                mapAssetWithEligibility[asset] ?: InterestEligibility.Ineligible.default()
            }
    }

    // limits
    override fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>> {
        return getLimitsForAssetsFlow()
            .asObservable().firstOrError()
    }

    override fun getLimitsForAssetsFlow(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, InterestLimits>>> {
        return interestLimitsStore.stream(refreshStrategy).mapData { interestLimits ->
            interestLimits.tickerLimits.entries.mapNotNull { (assetTicker, limits) ->
                assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let { asset ->

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, 1)
                    }

                    val minDepositFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.minDepositAmount.toBigInteger()
                    )
                    val maxWithdrawalFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.maxWithdrawalAmount.toBigInteger()
                    )

                    val interestLimit = InterestLimits(
                        interestLockUpDuration = limits.lockUpDuration,
                        nextInterestPayment = calendar.time,
                        minDepositFiatValue = minDepositFiatValue,
                        maxWithdrawalFiatValue = maxWithdrawalFiatValue
                    )

                    Pair(asset, interestLimit)
                }
            }.toMap()
        }
    }

    override fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits> {
        return getLimitsForAssets().map { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }
    }

    override fun getLimitsForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<InterestLimits>> {
        return getLimitsForAssetsFlow(refreshStrategy)
            .mapData { mapAssetWithLimits ->
                mapAssetWithLimits[asset]
                    ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
            }
    }

    // rate
    override fun getInterestRate(asset: AssetInfo): Single<Double> {
        return getInterestRateFlow(asset)
            .asObservable().firstOrError()
    }

    override fun getInterestRateFlow(asset: AssetInfo, refreshStrategy: FreshnessStrategy): Flow<DataResource<Double>> {
        return interestRateStore.stream(refreshStrategy.withKey(InterestRateStore.Key(asset.networkTicker)))
            .mapData { interestRateResponse -> interestRateResponse.rate }
    }

    // address
    override fun getAddress(asset: AssetInfo): Single<String> {
        return authenticator.authenticate { sessionToken ->
            interestApiService.getAddress(sessionToken.authHeader, asset.networkTicker)
                .map { interestAddressResponse -> interestAddressResponse.address }
        }
    }

    // activity
    override fun getActivity(asset: AssetInfo): Single<List<InterestActivity>> {
        return getActivityFlow(asset)
            .asObservable().firstOrError()
    }

    override fun getActivityFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<InterestActivity>>> {
        return paymentTransactionHistoryStore
            .stream(
                refreshStrategy.withKey(
                    PaymentTransactionHistoryStore.Key(product = INTEREST_PRODUCT_NAME, type = null)
                )
            )
            .mapData { interestActivityResponse ->
                interestActivityResponse.items
                    .filter { transaction ->
                        assetCatalogue.fromNetworkTicker(
                            transaction.amount.symbol
                        )?.networkTicker == asset.networkTicker
                    }.map { transaction ->
                        transaction.toInterestActivity(asset)
                    }
            }
    }

    // withdrawal
    override fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable {
        return authenticator.authenticateCompletable { sessionToken ->
            interestApiService.performWithdrawal(
                authHeader = sessionToken.authHeader,
                body = InterestWithdrawalBodyDto(
                    withdrawalAddress = address,
                    amount = amount.toBigInteger().toString(),
                    currency = asset.networkTicker
                )
            )
        }
    }

    companion object {
        const val INTEREST_PRODUCT_NAME = "savings"
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
