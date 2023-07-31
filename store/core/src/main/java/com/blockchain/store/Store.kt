package com.blockchain.store

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.internalnotifications.NotificationEvent
import kotlinx.coroutines.flow.Flow

internal typealias Millis = Long
typealias StoreId = String

/**
 * A Store is responsible for managing a particular data request.
 *
 * When you create an implementation of a Store via [SqlDelightBackedStoreBuilder], you provide it with a Fetcher, a function that defines how data will be fetched over network and with a Freshness.
 *
 * You then observe using [stream] with one of the available [Freshness]
 *
 * Example usage:
 *
 * ```
 * class LinkedCardsStore(
 *     private val paymentMethodsService: PaymentMethodsService,
 *     private val authenticator: Authenticator
 * ) : Store<List<CardResponse>, PaymentMethodsError> by SqlDelightBackedStoreBuilder().build(
 *     storeId = "LinkedCardsStore",
 *     fetcher = Fetcher.ofOutcome {
 *         paymentMethodsService.getCards()
 *     },
 *     dataSerializer = ListSerializer(CardResponse.serializer()),
 *     freshness = Freshness.ofMinutes(5L)
 * )
 *
 * class SettingsMviModel(val cardService: CardService) : MviViewModel {
 *     override fun viewCreated() {
 *         viewModelScope.launch {
 *             cardService.getLinkedCards(StoreRequest.Cached(forceRefresh = false))
 *                 .collect { storeResponse ->
 *                     updateState { prevState ->
 *                         prevState.copy(
 *                             isLoadingCards = storeResponse is StoreResponse.Loading,
 *                             cardsError = if (storeResponse is StoreResponse.Error) storeResponse.error else prevState.cardsError,
 *                             cards = if (storeResponse is StoreResponse.Data) storeResponse.data else prevState.cards
 *                         )
 *                     }
 *                 }
 *         }
 *     }
 *
 *     fun refreshCards() {
 *         viewModelScope.launch {
 *             updateState { prevState ->
 *                 prevState.copy(isLoadingCards = true)
 *             }
 *             val result = cardService.getLinkedCards(StoreRequest.Fresh).firstOutcome()
 *             // No need to handle StoreResponse.Data as the stream opened in viewCreated will be called with the updated StoreResponse.Data
 *             updateState { prevState ->
 *                 prevState.copy(
 *                     isLoadingCards = false,
 *                     cardsError = if (result is StoreResponse.Error) result.error else prevState.cardsError
 *                 )
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface Store<T : Any> {
    fun stream(request: FreshnessStrategy): Flow<DataResource<T>>
    fun markAsStale()
}

/**
 * A version of [Store] that takes in a Key object, you should use [SqlDelightBackedStoreBuilder.buildKeyed] to build and
 * use [Fetcher.Keyed] helper functions to create a fetcher and pass in a KSerializer<Key>
 *
 * See [Store] for more documentation and [PaymentMethodsEligibilityStore] for a working example.
 */
interface KeyedStore<K : Any, T : Any> {
    fun stream(request: KeyedFreshnessStrategy<K>): Flow<DataResource<T>>
    fun markAsStale(key: K)
    fun markStoreAsStale()
}

data class CacheConfiguration internal constructor(val flushEvents: List<NotificationEvent>) {
    companion object {
        fun default() = CacheConfiguration(emptyList())
        fun onLogout(): CacheConfiguration = CacheConfiguration(flushEvents = listOf(NotificationEvent.Logout))
        fun onLogin(): CacheConfiguration = CacheConfiguration(flushEvents = listOf(NotificationEvent.Login))
        fun onAnyTransaction(): CacheConfiguration =
            CacheConfiguration(
                flushEvents = listOf(
                    NotificationEvent.NonCustodialTransaction,
                    NotificationEvent.RewardsTransaction,
                    NotificationEvent.StakingTransaction,
                    NotificationEvent.TradingTransaction,
                )
            )

        fun onKycStatusChanged(): CacheConfiguration =
            CacheConfiguration(flushEvents = listOf(NotificationEvent.KycStatusChanged))

        fun on(events: List<NotificationEvent>): CacheConfiguration =
            CacheConfiguration(flushEvents = events)
    }

    operator fun plus(other: CacheConfiguration) =
        CacheConfiguration(other.flushEvents + flushEvents)
}
