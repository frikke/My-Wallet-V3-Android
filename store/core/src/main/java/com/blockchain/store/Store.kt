package com.blockchain.store

import kotlinx.coroutines.flow.*

typealias Millis = Long
typealias StoreId = String


/**
 * A Store is responsible for managing a particular data request.
 *
 * When you create an implementation of a Store via [SqlDelightBackedStoreBuilder], you provide it with a Fetcher, a function that defines how data will be fetched over network and with a Freshness.
 *
 * You then observe using [stream] with one of the available [StoreRequest]
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
 *         val authToken = authenticator.getAuthHeader().await()
 *         paymentMethodsService.getCards(authToken)
 *     },
 *     dataSerializer = ListSerializer(CardResponse.serializer()),
 *     freshness = Freshness.ofMinutes(5L)
 * )
 *
 * class SettingsMviModel(val paymentsDataManager: PaymentsDataManager) : MviViewModel {
 *     override fun viewCreated() {
 *         viewModelScope.launch {
 *             paymentsDataManager.getLinkedCards(StoreRequest.Cached(forceRefresh = false))
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
 *             val result = paymentsDataManager.getLinkedCards(StoreRequest.Fresh).firstOutcome()
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
interface Store<E : Any, T : Any> {
    fun stream(request: StoreRequest): Flow<StoreResponse<E, T>>
    fun markAsStale()
}

/**
 * A version of [Store] that takes in a Key object, you should use [SqlDelightBackedStoreBuilder.buildKeyed] to build and
 * use [Fetcher.Keyed] helper functions to create a fetcher and pass in a KSerializer<Key>
 *
 * See [Store] for more documentation and [PaymentMethodsEligibilityStore] for a working example.
 */
interface KeyedStore<K : Any, E : Any, T : Any> {
    fun stream(request: KeyedStoreRequest<K>): Flow<StoreResponse<E, T>>
    fun markAsStale(key: K)
}

/**
 * Defines the way that the [Store.stream] will operate:
 *   - [Fresh] will always skip cache at first, fetch and listen for future cache changes: `[Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(forceRefresh=true)] will get the latest cache, fetch and listen for future cache changes: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(forceRefresh=false)] will get the latest cache, and only fetch if there is no cached data or if the mediator decides to fetch, it will also listen for future cache changes:
 *      - should fetch == true: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *      - should fetch == false: `[Data(cache), Data(future cache change)]`
 */
sealed class StoreRequest {
    object Fresh : StoreRequest()
    data class Cached(val forceRefresh: Boolean) : StoreRequest()
}

/**
 * [Loading] : emitted exclusively when fetching from network, the next emitted Data or Error will be related to the network fetch and mean that Store is no longer Loading
 * [Data] : emitted when the fetcher completes successfully or when we get a Cached value
 * [Error] : emitted exclusively when fetching from network, when a Fetcher error has occurred
 */
sealed class StoreResponse<out E, out T> {
    object Loading : StoreResponse<Nothing, Nothing>()
    data class Data<out T>(val data: T) : StoreResponse<Nothing, T>() {

        // This is used internally to make StoreResponse.firstOutcome() work as expected,
        // allowing it to ignore the first cachedData when it's stale or it's doing a force refresh
        internal var isStale: Boolean = false
            private set

        internal constructor(data: T, isStale: Boolean) : this(data) {
            this.isStale = isStale
        }
    }
    data class Error<out E>(val error: E) : StoreResponse<E, Nothing>()
}

/**
 * Keyed version of [StoreRequest] used in [KeyedStore]
 * See [StoreRequest] and [KeyedStore] for more detailed documentation.
 */
sealed class KeyedStoreRequest<out K> {
    data class Fresh<out K>(val key: K) : KeyedStoreRequest<K>()
    data class Cached<out K>(val key: K, val forceRefresh: Boolean) : KeyedStoreRequest<K>()
}