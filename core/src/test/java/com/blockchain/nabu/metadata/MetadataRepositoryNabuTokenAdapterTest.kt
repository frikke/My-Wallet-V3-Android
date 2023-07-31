package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class MetadataRepositoryNabuTokenAdapterTest {

    @Test
    fun `before subscription, does not access the repository`() {
        val metadataRepository: MetadataRepository = mock()
        val createNabuToken: CreateNabuToken = mock()
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()

        MetadataRepositoryNabuTokenAdapter(accountCredentialsMetadata, createNabuToken).fetchNabuToken()
        verifyZeroInteractions(metadataRepository)
        verifyZeroInteractions(createNabuToken)
    }

    @Test
    fun `can get token from account metadata`() {
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        accountCredentialsMetadata.givenAccountMetadata(
            Maybe.just(
                BlockchainAccountCredentialsMetadata(
                    userId = "User1",
                    lifetimeToken = "TOKEN123",
                    null,
                    null
                )
            )
        )

        MetadataRepositoryNabuTokenAdapter(
            accountCredentialsMetadata,
            mock()
        ).fetchNabuToken()
            .test()
            .values()
            .single() `should be equal to` NabuOfflineToken(
            userId = "User1",
            token = "TOKEN123"
        )
    }

    @Test
    fun `should fetch metadata and map to NabuOfflineTokenResponse`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuLegacyCredentialsMetadata(id, lifetimeToken)
        val accountMetadata: AccountCredentialsMetadata = mock()

        accountMetadata.givenAccountMetadata(Maybe.just(offlineToken))
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            accountMetadata,
            mock()
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should be equal to` id
        token `should be equal to` lifetimeToken
    }

    @Test
    fun `if the metadata is not found, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken, true)
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.empty()
        )

        whenever(accountCredentialsMetadata.save(any())).thenReturn(
            Completable.complete()
        )

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            accountCredentialsMetadata,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(offlineToken))
            }

        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should be equal to` id
        token `should be equal to` lifetimeToken
        verify(accountCredentialsMetadata).save(
            NabuOfflineToken(
                "ID",
                "LIFETIME_TOKEN"
            )
        )
    }

    @Test
    fun `if the metadata is invalid, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken, true)
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(BlockchainAccountCredentialsMetadata.invalid())
        )

        whenever(accountCredentialsMetadata.save(any())).thenReturn(
            Completable.complete()
        )

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            accountCredentialsMetadata,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(offlineToken))
            }

        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should be equal to` id
        token `should be equal to` lifetimeToken
        verify(accountCredentialsMetadata).save(
            NabuOfflineToken(
                "ID",
                "LIFETIME_TOKEN"
            )
        )
    }

    @Test
    fun `should throw MetadataNotFoundException as token is invalid from create call`() {
        // Arrange
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(BlockchainAccountCredentialsMetadata(null, null, null, null))
        )
        whenever(accountCredentialsMetadata.save(any())).thenReturn(
            Completable.complete()
        )

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            accountCredentialsMetadata,
            mock {
                on { createNabuOfflineToken() }.thenReturn(
                    Single.just(
                        NabuOfflineTokenResponse(
                            "",
                            "",
                            true
                        )
                    )
                )
            }
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(MetadataNotFoundException::class.java)
    }

    @Test
    fun `if the metadata becomes available later, it is visible`() {
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.error(Throwable())
        )

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            accountCredentialsMetadata,
            mock()
        )
        nabuToken.fetchNabuToken().test()
            .assertNotComplete()
            .assertError(Throwable::class.java)

        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(NabuLegacyCredentialsMetadata("USER1", "TOKEN2"))
        )

        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN2"
            }
    }

    @Test
    fun `if the metadata is available, it does not update, proving cached`() {
        val accountCredentialsMetadata: AccountCredentialsMetadata = mock()
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(NabuLegacyCredentialsMetadata("USER1", "TOKEN1"))
        )

        val nabuToken = MetadataRepositoryNabuTokenAdapter(

            accountCredentialsMetadata,
            mock()
        )
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(
                NabuLegacyCredentialsMetadata(
                    "",
                    ""
                )
            )
        )

        whenever(accountCredentialsMetadata.load()).thenReturn(
            Maybe.just(
                NabuLegacyCredentialsMetadata(
                    "USER2",
                    "TOKEN2"
                )
            )
        )
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
    }
}

private fun AccountCredentialsMetadata.givenAccountMetadata(
    metadata: Maybe<CredentialMetadata>
): AccountCredentialsMetadata {
    whenever(
        load()
    ).thenReturn(metadata)
    return this
}
