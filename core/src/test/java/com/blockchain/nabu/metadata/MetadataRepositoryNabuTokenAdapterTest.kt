package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapFromMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuAccountMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuUserMetadata
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.amshove.kluent.`should be equal to`
import org.junit.Test

@OptIn(InternalSerializationApi::class)
class MetadataRepositoryNabuTokenAdapterTest {

    @Test
    fun `before subscription, does not access the repository`() {
        val metadataRepository: MetadataRepository = mock()
        val createNabuToken: CreateNabuToken = mock()

        MetadataRepositoryNabuTokenAdapter(metadataRepository, createNabuToken, mock()).fetchNabuToken()
        verifyZeroInteractions(metadataRepository)
        verifyZeroInteractions(createNabuToken)
    }

    @Test
    fun `can get token from metadata repository`() {
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(
            Maybe.just(
                NabuUserCredentialsMetadata(
                    userId = "User1",
                    lifetimeToken = "TOKEN123",
                    null,
                    null,
                )
            )
        )

        MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(false))
            }
        ).fetchNabuToken()
            .test()
            .values()
            .single() `should be equal to` NabuOfflineTokenResponse(
            userId = "User1",
            token = "TOKEN123"
        )
    }

    @Test
    fun `can get token from account metadata repository`() {
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(
            Maybe.just(NabuAccountCredentialsMetadata("User1", "TOKEN123", null, null))
        )

        MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(true))
            }
        ).fetchNabuToken()
            .test()
            .values()
            .single() `should be equal to` NabuOfflineTokenResponse(
            userId = "User1",
            token = "TOKEN123"
        )
    }

    @Test
    fun `should fetch metadata and map to NabuOfflineTokenResponse`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuUserCredentialsMetadata(id, lifetimeToken, null, null)
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(Maybe.just(offlineToken))
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(false))
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
    }

    @Test
    fun `should fetch account metadata and map to NabuOfflineTokenResponse`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuAccountCredentialsMetadata(id, lifetimeToken, null, null)
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(offlineToken))
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(true))
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
    }

    @Test
    fun `if the metadata is not found, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToNabuUserMetadata()
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(Maybe.empty()).expectSave(data)

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(offlineToken))
            },
            mock {
                on { enabled }.thenReturn(Single.just(false))
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
        verify(metadataRepository).saveMetadata(
            data,
            NabuUserCredentialsMetadata::class.java,
            NabuUserCredentialsMetadata::class.serializer(),
            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `if the account metadata is not found, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToNabuAccountMetadata()
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.empty()).expectSave(data)
        metadataRepository.givenUserMetaData(Maybe.empty()).expectSave(data)

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(offlineToken))
            },
            mock {
                on { enabled }.thenReturn(Single.just(true))
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
        verify(metadataRepository).saveMetadata(
            data,
            NabuAccountCredentialsMetadata::class.java,
            NabuAccountCredentialsMetadata::class.serializer(),
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `if the metadata is invalid, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeTokenFound = ""
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToNabuUserMetadata()
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(
            Maybe.just(NabuOfflineTokenResponse(id, lifetimeTokenFound).mapToNabuUserMetadata())
        ).expectSave(data)
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(offlineToken))
            },
            mock {
                on { enabled }.thenReturn(Single.just(false))
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
        verify(metadataRepository).saveMetadata(
            data,
            NabuUserCredentialsMetadata::class.java,
            NabuUserCredentialsMetadata::class.serializer(),
            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `if the account metadata is invalid, and user metadata is valid, it creates a new account metadata`() {
        // Arrange
        val id = "ID"
        val lifetimeTokenFound = ""
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToNabuUserMetadata()
        val metadataRepository: MetadataRepository = mock()
        val accountMetadata = NabuOfflineTokenResponse(id, lifetimeTokenFound).mapToNabuAccountMetadata()
        whenever(
            metadataRepository.loadMetadata(
                NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
                NabuAccountCredentialsMetadata::class.serializer(),
                NabuAccountCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.just(accountMetadata)).thenReturn(Maybe.just(offlineToken.mapToNabuAccountMetadata()))
        metadataRepository.givenUserMetaData(
            Maybe.just(NabuOfflineTokenResponse(id, lifetimeTokenFound).mapToNabuUserMetadata())
        ).expectSave(data)
        whenever(
            metadataRepository.saveMetadata(
                offlineToken.mapToNabuAccountMetadata(),
                NabuAccountCredentialsMetadata::class.java,
                NabuAccountCredentialsMetadata::class.serializer(),
                NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Completable.complete())

        val createNabuToken: CreateNabuToken = mock()
        whenever(createNabuToken.createNabuOfflineToken()).thenReturn(Single.just(offlineToken))

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            createNabuToken,
            mock {
                on { enabled }.thenReturn(Single.just(true))
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
        verify(metadataRepository).saveMetadata(
            offlineToken.mapToNabuAccountMetadata(),
            NabuAccountCredentialsMetadata::class.java,
            NabuAccountCredentialsMetadata::class.serializer(),
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `should throw MetadataNotFoundException as token is invalid from create call`() {
        // Arrange
        val offlineToken = NabuUserCredentialsMetadata("", "", null, null)
        val metadata = offlineToken.mapFromMetadata()
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(Maybe.empty()).expectSave(offlineToken)
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() }.thenReturn(Single.just(metadata))
            },
            mock {
                on { enabled }.thenReturn(Single.just(false))
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
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenUserMetaData(Maybe.empty())
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            givenCantCreate(),
            mock {
                on { enabled }.thenReturn(Single.just(false))
            }
        )
        nabuToken.fetchNabuToken().test()
            .assertNotComplete()
            .assertError(Throwable::class.java)

        metadataRepository.givenUserMetaData(Maybe.just(NabuUserCredentialsMetadata("USER1", "TOKEN2", null, null)))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN2"
            }
        metadataRepository.verifyJustLoadCalledNTimes(2, 2)
    }

    @Test
    fun `if the account metadata becomes available later, it is visible`() {
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.empty())

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            givenCantCreate(),
            mock {
                on { enabled }.thenReturn(Single.just(true))
            }
        )
        nabuToken.fetchNabuToken().test()
            .assertNotComplete()
            .assertError(Throwable::class.java)

        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata("USER1", "TOKEN2", null, null)))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN2"
            }
        metadataRepository.verifyJustLoadCalledNTimes(2, 2)
    }

    private fun givenCantCreate(): CreateNabuToken =
        mock {
            on { createNabuOfflineToken() }.thenReturn(Single.error(Throwable("Can't create")))
        }

    @Test
    fun `if the metadata is available, it does not update, proving cached`() {
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(
            Maybe.just(NabuUserCredentialsMetadata("USER1", "TOKEN1", null, null))
        )
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(false))
            }
        )
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata(null, null, null, null)))
        metadataRepository.givenUserMetaData(Maybe.just(NabuUserCredentialsMetadata("USER2", "TOKEN2", null, null)))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
        metadataRepository.verifyJustLoadCalledNTimes(1, 1)
    }

    @Test
    fun `if the account metadata is available, it does not update, proving cached`() {
        val metadataRepository: MetadataRepository = mock()
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata("USER1", "TOKEN1", null, null)))

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock(),
            mock {
                on { enabled }.thenReturn(Single.just(true))
            }
        )
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
        metadataRepository.givenAccountMetadata(Maybe.just(NabuAccountCredentialsMetadata("USER2", "TOKEN2", null, null)))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should be equal to` "USER1"
                this.token `should be equal to` "TOKEN1"
            }
        metadataRepository.verifyJustLoadCalledNTimes(0, 1)
    }

    @Test
    fun `if the account metadata errors, attempt to get user metadata instead`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToNabuUserMetadata()
        val metadataRepository: MetadataRepository = mock()
        whenever(
            metadataRepository.loadMetadata(
                NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
                NabuAccountCredentialsMetadata::class.serializer(),
                NabuAccountCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.error(Exception()))
        metadataRepository.givenUserMetaData(
            Maybe.just(NabuOfflineTokenResponse(id, lifetimeToken).mapToNabuUserMetadata())
        ).expectSave(data)

        val createNabuToken: CreateNabuToken = mock()
        whenever(createNabuToken.createNabuOfflineToken()).thenReturn(Single.just(offlineToken))

        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            createNabuToken,
            mock {
                on { enabled }.thenReturn(Single.just(true))
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
        metadataRepository.verifyJustLoadCalledNTimes(1, 1)
    }
}

@OptIn(InternalSerializationApi::class)
private fun MetadataRepository.givenUserMetaData(
    metadata: Maybe<NabuUserCredentialsMetadata>?
): MetadataRepository {
    whenever(
        loadMetadata(
            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
            NabuUserCredentialsMetadata::class.serializer(),
            NabuUserCredentialsMetadata::class.java
        )
    ).thenReturn(metadata)
    return this
}

@OptIn(InternalSerializationApi::class)
private fun MetadataRepository.givenAccountMetadata(
    metadata: Maybe<NabuAccountCredentialsMetadata>?
): MetadataRepository {
    whenever(
        loadMetadata(
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
            NabuAccountCredentialsMetadata::class.serializer(),
            NabuAccountCredentialsMetadata::class.java
        )
    ).thenReturn(metadata)
    return this
}

@OptIn(InternalSerializationApi::class)
private fun MetadataRepository.expectSave(data: NabuUserCredentialsMetadata): MetadataRepository {
    whenever(
        saveMetadata(
            data,
            NabuUserCredentialsMetadata::class.java,
            NabuUserCredentialsMetadata::class.serializer(),
            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    ).thenReturn(Completable.complete())
    return this
}

@OptIn(InternalSerializationApi::class)
private fun MetadataRepository.expectSave(data: NabuAccountCredentialsMetadata): MetadataRepository {
    whenever(
        saveMetadata(
            data,
            NabuAccountCredentialsMetadata::class.java,
            NabuAccountCredentialsMetadata::class.serializer(),
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
        )
    ).thenReturn(Completable.complete())
    return this
}

@OptIn(InternalSerializationApi::class)
private fun MetadataRepository.verifyJustLoadCalledNTimes(n: Int, nAccount: Int) {
    verify(this, times(n)).loadMetadata(
        NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
        NabuUserCredentialsMetadata::class.serializer(),
        NabuUserCredentialsMetadata::class.java
    )
    verify(this, times(nAccount)).loadMetadata(
        NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
        NabuAccountCredentialsMetadata::class.serializer(),
        NabuAccountCredentialsMetadata::class.java
    )
    verifyNoMoreInteractions(this)
}
