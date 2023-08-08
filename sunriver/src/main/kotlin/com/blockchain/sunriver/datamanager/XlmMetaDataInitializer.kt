package com.blockchain.sunriver.datamanager

import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import com.blockchain.rx.maybeCache
import com.blockchain.sunriver.derivation.deriveXlmAccountKeyPair
import com.blockchain.sunriver.toKeyPair
import com.blockchain.utils.thenMaybe
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.Seed
import com.blockchain.wallet.SeedAccess
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.util.encoders.Hex

internal class XlmMetaDataInitializer(
    private val defaultLabels: DefaultLabels,
    private val repository: MetadataRepository,
    private val seedAccess: SeedAccess,
    private val remoteLogger: RemoteLogger
) {
    /**
     * Will not prompt for second password.
     */
    internal val initWalletMaybe: Maybe<XlmMetaData> = Maybe.defer {
        Maybe.concat(
            load,
            createAndSave()
        ).firstElement()
    }

    /**
     * Might prompt for second password if required to generate the meta data.
     */
    internal val initWalletMaybePrompt: Maybe<XlmMetaData> = Maybe.defer {
        Maybe.concat(
            load,
            createAndSavePrompt()
        ).firstElement()
    }

    private val load: Maybe<XlmMetaData> = Maybe.defer {
        repository.load<XlmMetaData>(MetadataEntry.METADATA_XLM)
            .ignoreInvalidCipherTextException()
            .ignoreBadMetadata()
            .compareForLog()
    }.maybeCache()

    private fun createAndSave(): Maybe<XlmMetaData> = newXlmMetaData().saveSideEffect()

    private fun createAndSavePrompt(): Maybe<XlmMetaData> = newXlmMetaDataPrompt().saveSideEffect()

    private fun newXlmMetaData(): Maybe<XlmMetaData> = seedAccess.seed.deriveMetadata()

    private fun newXlmMetaDataPrompt(): Maybe<XlmMetaData> =
        seedAccess.seedPromptIfRequired.deriveMetadata()

    private fun Maybe<XlmMetaData>.compareForLog(): Maybe<XlmMetaData> =
        flatMap { loaded ->
            Maybe.concat(
                newXlmMetaData()
                    .doOnSuccess { expected ->
                        inspectLoadedData(loaded, expected)
                    }
                    .map { loaded },
                this
            ).firstElement()
        }

    /**
     * Logs any discrepancies between the expected first account, and the loaded first account.
     * If it cannot test for discrepancies (e.g., no seed available at the time) it does not log anything.
     */
    private fun inspectLoadedData(loaded: XlmMetaData, expected: XlmMetaData) {
        val expectedAccount = expected.accounts?.get(0)
        val loadedAccount = loaded.accounts?.get(0)
        if (expectedAccount?.publicKey != loadedAccount?.publicKey) {
            Throwable("Xlm metadata expected did not match that loaded").let {
                remoteLogger.logException(it)
                if (remoteLogger.isDebugBuild) {
                    // we want to know about this on a debug build
                    throw it
                }
            }
        }
    }

    private fun Maybe<XlmMetaData>.saveSideEffect(): Maybe<XlmMetaData> =
        flatMap { newData ->
            repository.save(
                newData,
                MetadataEntry.METADATA_XLM
            ).thenMaybe {
                Maybe.just(newData)
            }
        }

    private fun Maybe<Seed>.deriveMetadata(): Maybe<XlmMetaData> =
        map { seed ->
            val derived = deriveXlmAccountKeyPair(seed.hdSeed, 0)
            XlmMetaData(
                defaultAccountIndex = 0,
                accounts = listOf(
                    XlmAccount(
                        publicKey = derived.accountId,
                        _label = defaultLabels.getDefaultNonCustodialWalletLabel(),
                        _archived = false,
                        pubKey = String(
                            Hex.encode(derived.toKeyPair().publicKey)
                        )
                    )
                ),
                transactionNotes = emptyMap()
            )
        }

    fun updateAccountLabel(newLabel: String): Completable = load.map { metadata ->
        val account = metadata.accounts?.get(0) ?: throw IllegalStateException("Account not initialised")
        metadata.copy(
            accounts = listOf(
                account.copy(
                    _label = newLabel
                )
            )
        )
    }.saveSideEffect().ignoreElement()
}

private fun Maybe<XlmMetaData>.ignoreInvalidCipherTextException(): Maybe<XlmMetaData> =
    onErrorResumeNext {
        if (it is InvalidCipherTextException) {
            Maybe.empty()
        } else Maybe.error(it)
    }

private fun Maybe<XlmMetaData>.ignoreBadMetadata(): Maybe<XlmMetaData> =
    map {
        it.copy(
            accounts = it.accounts?.filterNot { acc -> acc.pubKey == null } ?: emptyList()
        )
    }.filter { !(it.accounts?.isEmpty() ?: true) }
