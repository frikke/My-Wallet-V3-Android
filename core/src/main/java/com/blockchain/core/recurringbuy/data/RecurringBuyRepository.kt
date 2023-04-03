package com.blockchain.core.recurringbuy.data

import com.blockchain.api.recurringbuy.data.RecurringBuyDto
import com.blockchain.api.recurringbuy.data.RecurringBuyFrequencyConfigDto
import com.blockchain.api.recurringbuy.data.RecurringBuyFrequencyConfigListDto
import com.blockchain.api.recurringbuy.data.RecurringBuyRequestDto
import com.blockchain.api.services.RecurringBuyApiService
import com.blockchain.core.recurringbuy.data.datasources.RecurringBuyFrequencyConfigStore
import com.blockchain.core.recurringbuy.data.datasources.RecurringBuyStore
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.recurringbuy.domain.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyOrder
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyRequest
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.core.recurringbuy.domain.model.isActive
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.map
import com.blockchain.store.filterListData
import com.blockchain.store.filterNotLoading
import com.blockchain.store.mapData
import com.blockchain.store.mapListDataNotNull
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toException
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull

internal class RecurringBuyRepository(
    private val rbStore: RecurringBuyStore,
    private val rbFrequencyConfigStore: RecurringBuyFrequencyConfigStore,
    private val recurringBuyApiService: RecurringBuyApiService,
    private val assetCatalogue: AssetCatalogue,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : RecurringBuyService {

    override suspend fun isEligible(): Boolean {
        return userFeaturePermissionService
            .isEligibleFor(feature = Feature.Buy)
            .filterNotLoading()
            .firstOrNull()
            ?.dataOrElse(false) ?: false
    }

    override fun recurringBuys(
        includeInactive: Boolean,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<RecurringBuy>>> {
        return rbStore.stream(freshnessStrategy)
            .mapListDataNotNull { recurringBuyDto -> recurringBuyDto.toDomain() }
            .filterListData { recurringBuy ->
                includeInactive || recurringBuy.isActive()
            }
    }

    override fun recurringBuys(
        asset: AssetInfo,
        includeInactive: Boolean,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<RecurringBuy>>> {
        return recurringBuys(
            includeInactive = includeInactive,
            freshnessStrategy = freshnessStrategy
        ).filterListData { recurringBuy ->
            recurringBuy.asset.networkTicker == asset.networkTicker
        }
    }

    override fun recurringBuy(
        id: String,
        includeInactive: Boolean,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<RecurringBuy>> {
        return recurringBuys(
            includeInactive = includeInactive,
            freshnessStrategy = freshnessStrategy
        ).filterListData { recurringBuy ->
            recurringBuy.id == id
        }.mapData {
            it.first()
        }.catch {
            emit(DataResource.Error(it.toException()))
        }
    }

    override suspend fun cancelRecurringBuy(recurringBuy: RecurringBuy) {
        recurringBuyApiService.cancel(id = recurringBuy.id)
        rbStore.markAsStale()
    }

    override fun frequencyConfig(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<EligibleAndNextPaymentRecurringBuy>>> {
        return rbFrequencyConfigStore.stream(freshnessStrategy)
            .mapData { it.toDomain() }
    }

    override suspend fun createOrder(
        request: RecurringBuyRequest
    ): Outcome<Exception, RecurringBuyOrder> {
        return recurringBuyApiService.createRecurringBuy(request = request.toRecurringBuyRequestDto())
            .map {
                it.toRecurringBuyOrder()
            }
            .doOnSuccess {
                if (it.state == RecurringBuyState.ACTIVE) {
                    rbStore.markAsStale()
                }
            }
    }

    // mappers
    private fun RecurringBuyDto.toDomain(): RecurringBuy? {
        val asset = assetCatalogue.assetInfoFromNetworkTicker(destinationCurrency) ?: return null
        val fiatCurrency = assetCatalogue.fiatFromNetworkTicker(inputCurrency) ?: return null
        return RecurringBuy(
            id = id,
            state = state.toRecurringBuyState(),
            recurringBuyFrequency = period.toRecurringBuyFrequency(),
            nextPaymentDate = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            paymentMethodType = paymentMethod.toPaymentMethodType(),
            amount = Money.fromMinor(fiatCurrency, inputValue.toBigInteger()),
            asset = asset,
            createDate = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            paymentMethodId = paymentMethodId
        )
    }

    private fun String.toRecurringBuyFrequency(): RecurringBuyFrequency =
        when (this) {
            RecurringBuyDto.DAILY -> RecurringBuyFrequency.DAILY
            RecurringBuyDto.WEEKLY -> RecurringBuyFrequency.WEEKLY
            RecurringBuyDto.BI_WEEKLY -> RecurringBuyFrequency.BI_WEEKLY
            RecurringBuyDto.MONTHLY -> RecurringBuyFrequency.MONTHLY
            else -> RecurringBuyFrequency.UNKNOWN
        }

    private fun String.toRecurringBuyState(): RecurringBuyState =
        when (this) {
            RecurringBuyDto.ACTIVE -> RecurringBuyState.ACTIVE
            RecurringBuyDto.INACTIVE -> RecurringBuyState.INACTIVE
            else -> throw IllegalStateException("Unsupported recurring state")
        }

    private fun RecurringBuyFrequencyConfigListDto.toDomain(): List<EligibleAndNextPaymentRecurringBuy> =
        this.nextPayments.map {
            EligibleAndNextPaymentRecurringBuy(
                frequency = it.period.toRecurringBuyFrequency(),
                nextPaymentDate = it.nextPayment,
                eligibleMethods = it.eligibleMethods.map { it.toPaymentMethodTypeDomain() }
            )
        }

    private fun String.toPaymentMethodTypeDomain(): PaymentMethodType = when (this) {
        RecurringBuyFrequencyConfigDto.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
        RecurringBuyFrequencyConfigDto.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        RecurringBuyFrequencyConfigDto.FUNDS -> PaymentMethodType.FUNDS
        else -> PaymentMethodType.UNKNOWN
    }

    private fun RecurringBuyDto.toRecurringBuyOrder(): RecurringBuyOrder =
        RecurringBuyOrder(
            id = this.id,
            state = this.state.toRecurringBuyState()
        )

    private fun RecurringBuyRequest.toRecurringBuyRequestDto(): RecurringBuyRequestDto = run {
        RecurringBuyRequestDto(
            orderId = orderId,
            inputValue = inputValue,
            inputCurrency = inputCurrency,
            destinationCurrency = destinationCurrency,
            paymentMethod = paymentMethod,
            period = period,
            nextPayment = nextPayment,
            paymentMethodId = paymentMethodId
        )
    }
}
