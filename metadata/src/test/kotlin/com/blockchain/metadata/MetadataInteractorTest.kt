package com.blockchain.metadata

import com.blockchain.data.DataResource
import com.blockchain.metadata.data.MetadataStore
import com.blockchain.testutils.FakeHttpExceptionFactory
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataApiService
import info.blockchain.wallet.metadata.MetadataDerivation
import info.blockchain.wallet.metadata.data.MetadataResponse
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class MetadataInteractorTest {

    private lateinit var metadataInteractor: MetadataInteractor
    private val address = "1JxR3UVbSFShHB7YUnBKc3WGviXzELB7FA"

    private val fakeMetadataResponse = MetadataResponse(
        version = 1,
        payload = "/FE353C/kzLs0kkU7NyOVExrT0yLIGgHYYSMKb8PCVk=",
        signature = "H0SgSn2QqiJAkVW6XuVZOTur6y8KlQ0qbLhK0oL6/PS3fy7TSSBYCEWk3nlYJyQD9IYwYZK5yGFCxQ55asy+3y4=",
        prevMagicHash = null,
        typeId = -1,
        createdAt = 1583754936000,
        updatedAt = 1583754936000,
        address = address
    )

    private val metadataDerivation = MetadataDerivation()
    private val fakeMetadata = Metadata.newInstance(
        metaDataHDNode = metadataDerivation.deserializeMetadataNode(
            "xprv9vM7oGsuM9zGW2tneNriS8NJF6DNrZEK" +
                "vYMXSwP8SJNJRUuX6iXjZLQCCy52cXJKKb6XwWF3vr6mQCyy9d5msL9TrycrBmbPibKd2LhzjDW"
        ),
        type = 6,
        metadataDerivation = metadataDerivation
    )

    private lateinit var metadataApiService: MetadataApiService
    private lateinit var metadataStore: MetadataStore

    @Before
    fun setUp() {
        metadataApiService = mock()
        metadataStore = mock()
        metadataInteractor = MetadataInteractor(metadataApiService, metadataStore, mock())
    }

    @Test
    fun `fetchMagic with success`() {
        whenever(
            metadataApiService.getMetadata(address)
        ).thenReturn(
            Single.just(fakeMetadataResponse)
        )

        val test = metadataInteractor.fetchMagic(address).test()

        test.assertComplete()
        test.assertValueCount(1)
    }

    @Test
    fun `fetchMagic failure, failure is propagated`() {
        whenever(
            metadataApiService.getMetadata(address)
        ).thenReturn(
            Single.error(Error())
        )
        val test = metadataInteractor.fetchMagic(address).test()

        test.assertNotComplete()
        test.assertNoValues()
    }

    @Test
    fun `get metadata with error 404, empty should be returned`() {
        whenever(
            metadataStore.stream(any())
        ).thenReturn(
            flowOf(DataResource.Error(FakeHttpExceptionFactory.httpExceptionWith(404)))
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).isEmpty.test().await()

        test.assertValueAt(0, true)
        test.assertComplete()
    }

    @Test
    fun `get metadata with error different than 404, error should be returned`() {
        whenever(
            metadataStore.stream(any())
        ).thenReturn(
            flowOf(DataResource.Error(FakeHttpExceptionFactory.httpExceptionWith(400)))
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).test().await()

        test.assertError {
            it is HttpException && it.code() == 400
        }
        test.assertNotComplete()
    }

    @Test
    fun `get metadata with success`() {

        whenever(
            metadataStore.stream(any())
        ).thenReturn(
            flowOf(
                DataResource.Data(fakeMetadataResponse)
            )
        )

        val test = metadataInteractor.loadRemoteMetadata(fakeMetadata).test().await()
            .waitForCompletionWithoutErrors()

        test.assertValueAt(0, "{\"trades\":[]}")
    }
}
