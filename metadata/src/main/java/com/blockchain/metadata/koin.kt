package com.blockchain.metadata

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.metadata.data.MetadataStore
import info.blockchain.wallet.metadata.MetadataDerivation
import org.koin.dsl.bind
import org.koin.dsl.module

val metadataModule = module {

    scope(payloadScopeQualifier) {
        scoped {
            MetadataManager(
                walletPayloadService = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                remoteLogger = get()
            )
        }.bind(MetadataService::class)

        scoped {
            MetadataRepositoryAdapter(
                metadataManager = get(),
                json = get()
            )
        }.bind(MetadataRepository::class)

        scoped {
            MetadataInteractor(
                metadataApiService = get(),
                metadataStore = get(),
                notificationTransmitter = get()
            )
        }
    }
    single {
        MetadataStore(
            metadataApiService = get(),
        )
    }
}
