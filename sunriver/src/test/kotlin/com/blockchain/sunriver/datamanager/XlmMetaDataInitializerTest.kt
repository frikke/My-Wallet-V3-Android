package com.blockchain.sunriver.datamanager

import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.serialization.fromJson
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.Seed
import com.blockchain.wallet.SeedAccess
import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.github.novacrypto.bip39.SeedCalculator
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.amshove.kluent.`should be equal to`
import org.junit.Test

@InternalSerializationApi
class XlmMetaDataInitializerTest {

    private val remoteLogger: RemoteLogger = mock()

    @Test
    fun `no interactions before subscribe`() {
        val defaultLabels = mock<DefaultLabels>()
        val repository = mock<MetadataRepository>()
        val seedAccess = mock<SeedAccess>()

        XlmMetaDataInitializer(
            defaultLabels,
            repository,
            seedAccess,
            remoteLogger
        ).apply {
            initWalletMaybePrompt
            initWalletMaybe
        }
        verifyZeroInteractions(defaultLabels)
        verifyZeroInteractions(repository)
        verifyZeroInteractions(seedAccess)
    }

    @Test
    fun `if the meta data is missing, it will create it`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data is missing, it will create it, with second password if necessary`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedPresentOnlyWithSecondPasswordFor(
                mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"
            ),
            remoteLogger
        )
            .initWalletMaybePrompt
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data is missing, it will create it - alternative values`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GAVXVW5MCK7Q66RIBWZZKZEDQTRXWCZUP4DIIFXCCENGW2P6W4OA34RH",
                    label = "The Lumen Wallet",
                    pubKey = "2b7adbac12bf0f7a280db395648384e37b0b347f068416e2111a6b69feb71c0d",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("The Lumen Wallet"),
            repository,
            givenSeedFor(
                mnemonic = "resource asthma orphan phone ice canvas " +
                    "fire useful arch jewel impose vague theory cushion top"
            ),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data is there, it will not create it`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(expectedData)
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data is there, it will not create it, initWalletMaybePrompt`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "!@3",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(expectedData)
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenNoSeed(),
            remoteLogger
        )
            .initWalletMaybePrompt
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the save fails, the error bubbles up`() {
        val repository = mock<MetadataRepository> {
            on {
                saveMetadata(
                    any(),
                    any(),
                    eq(XlmMetaData::class.serializer()),
                    eq(MetadataEntry.METADATA_XLM)
                )
            }.thenReturn(Completable.error(Exception("Save fail")))
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertFailure(Exception::class.java)

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the seed is not present when it needs to create it, return empty`() {
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenNoSeed(),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertComplete()
            .assertValueCount(0)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `initWalletMaybePrompt - if the seed is not present when it needs to create it, return empty`() {
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyLoad()
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenNoSeed(),
            remoteLogger
        )
            .initWalletMaybePrompt
            .test()
            .assertComplete()
            .assertValueCount(0)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the seed is not present, but it doesn't need it, then there is no error`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    _archived = false,
                    pubKey = "@!£2",
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(expectedData)
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenNoSeed(),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data has no accounts, recreate it`() {
        val badData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = emptyList(),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(badData)
        }
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data account is null, recreate it`() {
        val badData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = null,
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(badData)
        }
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data account is empty object, recreate it`() {
        val badData = XlmMetaData::class.fromJson("{}", Json { explicitNulls = false })
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(badData)
        }
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GC3MMSXBWHL6CPOAVERSJITX7BH76YU252WGLUOM5CJX3E7UCYZBTPJQ",
                    label = "My Lumen Wallet X",
                    pubKey = "b6c64ae1b1d7e13dc0a92324a277f84fff629aeeac65d1cce8937d93f4163219",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet X"),
            repository,
            givenSeedFor(
                mnemonic = "bench hurt jump file august wise shallow faculty impulse spring exact slush " +
                    "thunder author capable act festival slice deposit sauce coconut afford frown better"
            ),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertSaved(expectedData)
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `if the meta data is there, but the first account does not match the expected values, log warning`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    _archived = false,
                    pubKey = "3998db92ebfd1e8c190c9845ea006cd094fad28088ac91847ec994079cc9906d",
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(expectedData)
        }
        XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(
                mnemonic = "cable spray genius state float twenty onion head street palace net private " +
                    "method loan turn phrase state blanket interest dry amazing dress blast tube"
            ),
            remoteLogger
        )
            .initWalletMaybe
            .test()
            .assertNoErrors()
            .assertComplete()
            .values() `should be equal to` listOf(expectedData)

        repository.assertNothingSaved()
        repository.assertLoaded()

        verify(remoteLogger).logException(any(), any())

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `caching - without create, if you load twice, you get same cached result and just one repository load call`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    _archived = false,
                    pubKey = "!@3231"
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            loads(expectedData)
        }
        val initializer = XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenNoSeed(),
            remoteLogger
        )

        (1..2).forEach {
            initializer
                .initWalletMaybePrompt
                .test()
                .assertNoErrors()
                .assertComplete()
                .values() `should be equal to` listOf(expectedData)

            initializer
                .initWalletMaybe
                .test()
                .assertNoErrors()
                .assertComplete()
                .values() `should be equal to` listOf(expectedData)
        }

        repository.assertNothingSaved()
        repository.assertLoaded()

        verifyNoMoreInteractions(remoteLogger)

        assertSingleMetaDataLoad(repository)
    }

    @Test
    fun `caching - after create, if you load twice, you get same cached result and just one repository load call`() {
        val expectedData = XlmMetaData(
            defaultAccountIndex = 0,
            accounts = listOf(
                XlmAccount(
                    publicKey = "GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6",
                    label = "My Lumen Wallet",
                    pubKey = "e3726830a0b60cb5f52c844cffcd4eed65eba5c155e89b26411562724e71e544",
                    _archived = false
                )
            ),
            transactionNotes = emptyMap()
        )
        val repository = mock<MetadataRepository> {
            successfulSave()
            emptyAtFirstThenLoads(expectedData)
        }
        val initializer = XlmMetaDataInitializer(
            givenDefaultXlmLabel("My Lumen Wallet"),
            repository,
            givenSeedFor(mnemonic = "illness spike retreat truth genius clock brain pass fit cave bargain toe"),
            remoteLogger
        )

        (1..5).forEach {
            initializer
                .initWalletMaybe
                .test()
                .assertNoErrors()
                .assertComplete()
                .values() `should be equal to` listOf(expectedData)
        }

        repository.assertSaved(expectedData)
        verify(repository, times(2)).loadMetadata(
            metadataType = MetadataEntry.METADATA_XLM, XlmMetaData::class.serializer(), XlmMetaData::class.java
        )
        verify(repository, times(2)).loadMetadata(
            any(), eq(XlmMetaData::class.serializer()), eq(XlmMetaData::class.java)
        )

        verifyNoMoreInteractions(remoteLogger)
    }

    private fun givenSeedFor(mnemonic: String): SeedAccess =
        object : SeedAccess {

            override val seed: Maybe<Seed>
                get() = mnemonic.toSeed()

            override val seedPromptIfRequired: Maybe<Seed>
                get() = throw Exception("Unexpected")

            override fun seed(validatedSecondPassword: String?): Maybe<Seed> {
                throw Exception("Unexpected")
            }
        }

    private fun givenSeedPresentOnlyWithSecondPasswordFor(mnemonic: String): SeedAccess =
        object : SeedAccess {

            override val seed: Maybe<Seed>
                get() = throw Exception("Unexpected")

            override val seedPromptIfRequired: Maybe<Seed>
                get() = mnemonic.toSeed()

            override fun seed(validatedSecondPassword: String?): Maybe<Seed> {
                throw Exception("Unexpected")
            }
        }

    private fun String.toSeed() =
        Maybe.just(
            Seed(
                hdSeed = SeedCalculator().calculateSeed(this, "")
            )
        )

    private fun givenNoSeed(): SeedAccess =
        object : SeedAccess {

            override val seed: Maybe<Seed>
                get() = Maybe.empty()

            override val seedPromptIfRequired: Maybe<Seed>
                get() = Maybe.empty()

            override fun seed(validatedSecondPassword: String?): Maybe<Seed> {
                throw Exception("Unexpected")
            }
        }

    private fun MetadataRepository.assertNothingSaved() {
        verify(this, never()).saveMetadata(any(), any(), eq(XlmMetaData::class.serializer()), any())
    }

    private fun MetadataRepository.assertLoaded() {
        verify(this).loadMetadata(MetadataEntry.METADATA_XLM, XlmMetaData::class.serializer(), XlmMetaData::class.java)
    }

    private fun assertSingleMetaDataLoad(repository: MetadataRepository) {
        verify(repository).loadMetadata(any(), eq(XlmMetaData::class.serializer()), eq(XlmMetaData::class.java))
    }

    private fun MetadataRepository.assertSaved(
        value: XlmMetaData
    ) {
        verify(this).saveMetadata(
            eq(
                value
            ),
            eq(XlmMetaData::class.java),
            eq(XlmMetaData::class.serializer()),
            eq(MetadataEntry.METADATA_XLM)
        )
    }

    private fun KStubbing<MetadataRepository>.emptyLoad() {
        on {
            loadMetadata(
                MetadataEntry.METADATA_XLM, XlmMetaData::class.serializer(), XlmMetaData::class.java
            )
        }.thenReturn(Maybe.empty())
    }

    private fun KStubbing<MetadataRepository>.loads(expectedData: XlmMetaData) {
        on {
            loadMetadata(
                MetadataEntry.METADATA_XLM, XlmMetaData::class.serializer(), XlmMetaData::class.java
            )
        }.thenReturn(
            Maybe.just(
                expectedData
            )
        )
    }

    private fun KStubbing<MetadataRepository>.emptyAtFirstThenLoads(expectedData: XlmMetaData) {
        var count = 1
        on {
            loadMetadata(
                MetadataEntry.METADATA_XLM, XlmMetaData::class.serializer(), XlmMetaData::class.java
            )
        }.thenReturn(
            Maybe.defer {
                if (count-- > 0) {
                    Maybe.empty()
                } else {
                    Maybe.just(expectedData)
                }
            }
        )
    }

    private fun KStubbing<MetadataRepository>.successfulSave() {
        on {
            saveMetadata(
                any(),
                any(),
                eq(XlmMetaData::class.serializer()),
                eq(MetadataEntry.METADATA_XLM)
            )
        }.thenReturn(Completable.complete())
    }

    private fun givenDefaultXlmLabel(defaultLabel: String): DefaultLabels =
        mock {
            on { getDefaultNonCustodialWalletLabel() }.thenReturn(defaultLabel)
        }
}
