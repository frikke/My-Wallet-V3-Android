package com.blockchain.core.auth.metadata

import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.junit.Rule
import org.junit.Test

@OptIn(InternalSerializationApi::class)
class WalletCredentialsMetadataUpdaterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val metadataRepository: MetadataRepository = mock()
    private val payloadDataManager: PayloadDataManager = mock()

    private val subject = WalletCredentialsMetadataUpdater(
        metadataRepository,
        payloadDataManager
    )

    @Test
    fun `no metadata stored`() {
        whenever(payloadDataManager.guid).thenReturn(GUID_1)
        whenever(payloadDataManager.tempPassword).thenReturn(PASSWORD_1)
        whenever(payloadDataManager.sharedKey).thenReturn(KEY_1)

        whenever(
            metadataRepository.loadMetadata(
                MetadataEntry.WALLET_CREDENTIALS,
                WalletCredentialsMetadata::class.serializer(),
                WalletCredentialsMetadata::class.java
            )
        ).thenReturn(
            Maybe.empty()
        )

        // This is nasty. I'd like a much better way of testing for chained subscriptions TODO
        var subFlag = false
        val updateResult = Completable.complete()
            .doOnSubscribe { subFlag = true }

        whenever(
            metadataRepository.saveMetadata(
                WalletCredentialsMetadata(GUID_1, PASSWORD_1, KEY_1),
                WalletCredentialsMetadata::class.java,
                WalletCredentialsMetadata::class.serializer(),
                MetadataEntry.WALLET_CREDENTIALS,
            )
        ).thenReturn(updateResult)

        subject.checkAndUpdate()
            .test()

        verify(payloadDataManager).guid
        verify(payloadDataManager).tempPassword
        verify(payloadDataManager).sharedKey

        verify(metadataRepository).loadMetadata(
            MetadataEntry.WALLET_CREDENTIALS,
            WalletCredentialsMetadata::class.serializer(),
            WalletCredentialsMetadata::class.java
        )
        verify(metadataRepository).saveMetadata(
            WalletCredentialsMetadata(GUID_1, PASSWORD_1, KEY_1),
            WalletCredentialsMetadata::class.java,
            WalletCredentialsMetadata::class.serializer(),
            MetadataEntry.WALLET_CREDENTIALS,
        )

        assert(subFlag)

        verifyNoMoreInteractions(metadataRepository)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `metadata does not match stored`() {
        whenever(payloadDataManager.guid).thenReturn(GUID_1)
        whenever(payloadDataManager.tempPassword).thenReturn(PASSWORD_1)
        whenever(payloadDataManager.sharedKey).thenReturn(KEY_1)

        whenever(
            metadataRepository.loadMetadata(
                MetadataEntry.WALLET_CREDENTIALS,
                WalletCredentialsMetadata::class.serializer(),
                WalletCredentialsMetadata::class.java
            )
        ).thenReturn(
            Maybe.just(
                WalletCredentialsMetadata(
                    GUID_2,
                    PASSWORD_1,
                    KEY_1
                )
            )
        )

        subject.checkAndUpdate().test()

        verify(payloadDataManager).guid
        verify(payloadDataManager).tempPassword
        verify(payloadDataManager).sharedKey

        verify(metadataRepository).loadMetadata(
            MetadataEntry.WALLET_CREDENTIALS,
            WalletCredentialsMetadata::class.serializer(),
            WalletCredentialsMetadata::class.java
        )
        verify(metadataRepository).saveMetadata(
            WalletCredentialsMetadata(GUID_1, PASSWORD_1, KEY_1),
            WalletCredentialsMetadata::class.java,
            WalletCredentialsMetadata::class.serializer(),
            MetadataEntry.WALLET_CREDENTIALS,
        )
        verifyNoMoreInteractions(metadataRepository)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `metadata matches stored`() {
        whenever(payloadDataManager.guid).thenReturn(GUID_1)
        whenever(payloadDataManager.tempPassword).thenReturn(PASSWORD_1)
        whenever(payloadDataManager.sharedKey).thenReturn(KEY_1)

        whenever(
            metadataRepository.loadMetadata(
                MetadataEntry.WALLET_CREDENTIALS,
                WalletCredentialsMetadata::class.serializer(),
                WalletCredentialsMetadata::class.java
            )
        ).thenReturn(
            Maybe.just(
                WalletCredentialsMetadata(
                    GUID_1,
                    PASSWORD_1,
                    KEY_1
                )
            )
        )

        subject.checkAndUpdate().test()

        verify(payloadDataManager).guid
        verify(payloadDataManager).tempPassword
        verify(payloadDataManager).sharedKey

        verify(metadataRepository).loadMetadata(
            MetadataEntry.WALLET_CREDENTIALS,
            WalletCredentialsMetadata::class.serializer(),
            WalletCredentialsMetadata::class.java
        )

        verifyNoMoreInteractions(metadataRepository)
        verifyNoMoreInteractions(payloadDataManager)
    }

    companion object {
        private const val GUID_1 = "12334442341q3-134234-1234"
        private const val GUID_2 = "1u3irwqp3r1q3-134234-1234"
        private const val PASSWORD_1 = "change me"
        private const val KEY_1 = "980886878687978"
    }
}
