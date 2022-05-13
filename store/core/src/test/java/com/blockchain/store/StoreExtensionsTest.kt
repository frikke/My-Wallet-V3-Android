package com.blockchain.store

import app.cash.turbine.test
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class StoreExtensionsTest {

    private val subject = MutableSharedFlow<StoreResponse<Throwable, Item>>()

    @Test
    fun `firstOutcome should filter out Loading and Data that is cached and will be fetched next`() = runTest {
        flow { emit(subject.firstOutcome()) }.test {
            subject.emit(StoreResponse.Data(Item(1), isStale = true))
            expectNoEvents()
            subject.emit(StoreResponse.Loading)
            expectNoEvents()
            val item = Item(1)
            subject.emit(StoreResponse.Data(item, isStale = false))
            assertEquals(Outcome.Success(item), awaitItem())
            awaitComplete()
        }
    }

}