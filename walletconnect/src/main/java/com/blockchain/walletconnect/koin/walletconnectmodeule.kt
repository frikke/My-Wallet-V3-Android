package com.blockchain.walletconnect.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.walletConnectFeatureFlag
import com.blockchain.walletconnect.data.EthWalletAddressProvider
import com.blockchain.walletconnect.data.WalletConnectMetadataRepository
import com.blockchain.walletconnect.data.WalletConnectService
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import org.koin.dsl.bind
import org.koin.dsl.module

val walletConnectModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            WalletConnectService(
                sessionRepository = get(),
                client = get(),
                lifecycleObservable = get(),
                featureFlag = get(walletConnectFeatureFlag),
                walletConnectAccountProvider = get(),
            )
        }.bind(WalletConnectServiceAPI::class).bind(WalletConnectUrlValidator::class)

        factory {
            EthWalletAddressProvider(
                coincore = get()
            )
        }.bind(WalletConnectAddressProvider::class)

        factory {
            WalletConnectMetadataRepository(
                metadataManager = get()
            )
        }.bind(SessionRepository::class)
    }
}
