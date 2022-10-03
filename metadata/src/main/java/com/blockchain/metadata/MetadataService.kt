package com.blockchain.metadata

import info.blockchain.wallet.keys.MasterKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe

interface MetadataService {
    fun decryptAndSetupMetadata(): Completable
    fun attemptMetadataSetup(): Completable

    fun metadataForMasterKey(
        masterKey: MasterKey,
        type: MetadataEntry
    ): Maybe<String>

    fun reset()
}
