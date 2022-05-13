package com.blockchain.nabu.metadata

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.junit.Test
import org.mockito.Mockito

class AccountCredentialsMetadataTest {

    private val accountMetadataMigrationFF: FeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(true))
    }
    private val metadataRepository: MetadataRepository = mock()

    private val subject = AccountCredentialsMetadata(
        metadataRepository = metadataRepository,
        accountMetadataMigrationFF = accountMetadataMigrationFF
    )

    @Test
    fun `when blockchain data have been created and valid, they should returned upon load`() {
        val metadata = BlockchainAccountCredentialsMetadata(
            userId = "id",
            lifetimeToken = "lifetimeToken",
            exchangeUserId = "exchangeUserId",
            exchangeLifetimeToken = "exchangeLifetimeToken"
        )

        whenever(
            metadataRepository.load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        ).thenReturn(
            Maybe.just(
                metadata
            )
        )

        val test = subject.load().test()
        test.assertValue {
            it == metadata
        }

        Mockito.verify(metadataRepository)
            .load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)

        Mockito.verifyNoMoreInteractions(metadataRepository)
    }

    @Test
    fun `when account data haven't been created, legacy data should be requested, returned and migrated`() {
        val legacyMetadata = NabuLegacyCredentialsMetadata(
            userId = "id",
            lifetimeToken = "lifetimeToken"
        )

        whenever(
            metadataRepository.load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        ).thenReturn(
            Maybe.empty()
        )

        whenever(
            metadataRepository.load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)
        ).thenReturn(
            Maybe.just(legacyMetadata)
        )

        whenever(metadataRepository.saveMetadata(any(), any(), any(), any())).thenReturn(Completable.complete())

        val test = subject.load().test()

        test.assertValue {
            it == legacyMetadata
        }

        Mockito.verify(metadataRepository)
            .load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        Mockito.verify(metadataRepository).load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)

        Mockito.verify(
            metadataRepository
        ).save(
            BlockchainAccountCredentialsMetadata(
                userId = "id",
                lifetimeToken = "lifetimeToken",
                exchangeLifetimeToken = null,
                exchangeUserId = null
            ),
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        )
        Mockito.verifyNoMoreInteractions(metadataRepository)
    }

    @Test
    fun `when account or legacy metadata haven't been created empty is returned`() {

        whenever(
            metadataRepository.load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        ).thenReturn(
            Maybe.empty()
        )

        whenever(
            metadataRepository.load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)
        ).thenReturn(
            Maybe.empty()
        )

        val test = subject.load().test()

        test.assertResult()

        Mockito.verify(metadataRepository)
            .load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        Mockito.verify(metadataRepository).load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)

        Mockito.verifyNoMoreInteractions(metadataRepository)
    }

    @Test
    fun `when account metadata are invalid but legacy have been created, new metadata should updated correctly`() {

        whenever(
            metadataRepository.load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        ).thenReturn(
            Maybe.just(
                BlockchainAccountCredentialsMetadata(
                    userId = null,
                    lifetimeToken = null,
                    exchangeUserId = "Lakis",
                    exchangeLifetimeToken = "exchangeLifetimeToken"
                )
            )
        )

        whenever(metadataRepository.saveMetadata(any(), any(), any(), any())).thenReturn(Completable.complete())

        whenever(
            metadataRepository.load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)
        ).thenReturn(
            Maybe.just(
                NabuLegacyCredentialsMetadata(
                    userId = "123",
                    lifetimeToken = "lifetimetoken"
                )
            )
        )

        val test = subject.load().test()

        test.assertValue {
            it == NabuLegacyCredentialsMetadata(
                userId = "123",
                lifetimeToken = "lifetimetoken"
            )
        }

        Mockito.verify(metadataRepository)
            .load<BlockchainAccountCredentialsMetadata>(MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS)
        Mockito.verify(metadataRepository).load<NabuLegacyCredentialsMetadata>(MetadataEntry.NABU_LEGACY_CREDENTIALS)
        Mockito.verify(metadataRepository).save(
            BlockchainAccountCredentialsMetadata(
                userId = "123",
                lifetimeToken = "lifetimetoken",
                exchangeUserId = "Lakis",
                exchangeLifetimeToken = "exchangeLifetimeToken"
            ),
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        )

        Mockito.verifyNoMoreInteractions(metadataRepository)
    }

    @Test
    fun `when ff is off, legacy metadata should be saved`() {

        whenever(accountMetadataMigrationFF.enabled).thenReturn(Single.just(false))
        whenever(metadataRepository.saveMetadata(any(), any(), any(), any())).thenReturn(Completable.complete())

        val test = subject.saveAndReturn(NabuOfflineTokenResponse("123", "546")).test()
        val metadata = NabuLegacyCredentialsMetadata(
            userId = "123",
            lifetimeToken = "546"
        )

        test.assertValue {
            it == metadata
        }

        Mockito.verify(metadataRepository).save(
            metadata, MetadataEntry.NABU_LEGACY_CREDENTIALS
        )
    }

    @Test
    fun `when ff is on, account metadata should be saved`() {

        whenever(accountMetadataMigrationFF.enabled).thenReturn(Single.just(true))
        whenever(metadataRepository.saveMetadata(any(), any(), any(), any())).thenReturn(Completable.complete())

        val test = subject.saveAndReturn(NabuOfflineTokenResponse("123", "546")).test()

        val metadata = BlockchainAccountCredentialsMetadata(
            userId = "123",
            lifetimeToken = "546",
            exchangeLifetimeToken = null,
            exchangeUserId = null
        )

        test.assertValue {
            it == metadata
        }

        Mockito.verify(metadataRepository).save(
            metadata, MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        )
    }
}
