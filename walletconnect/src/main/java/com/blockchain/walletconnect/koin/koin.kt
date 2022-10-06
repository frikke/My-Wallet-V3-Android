package com.blockchain.walletconnect.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.walletconnect.data.EthWalletAddressProvider
import com.blockchain.walletconnect.data.SignRequestHandler
import com.blockchain.walletconnect.data.WalletConnectMetadataRepository
import com.blockchain.walletconnect.data.WalletConnectService
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.ui.dapps.DappsListModel
import com.blockchain.walletconnect.ui.networks.SelectNetworkViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module

val walletConnectModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            WalletConnectService(
                sessionRepository = get(),
                client = get(),
                analytics = get(),
                ethRequestSign = get(),
                lifecycleObservable = get(),
                walletConnectAccountProvider = get(),
                ethSendTransactionRequest = get()
            )
        }.apply {
            bind(WalletConnectServiceAPI::class)
            bind(WalletConnectUrlValidator::class)
        }

        factory {
            SignRequestHandler(
                accountProvider = get()
            )
        }.apply {
            bind(EthRequestSign::class)
            bind(EthSendTransactionRequest::class)
        }

        factory {
            EthWalletAddressProvider(
                coincore = get()
            )
        }.apply {
            bind(WalletConnectAddressProvider::class)
            bind(WalletConnectEthAccountProvider::class)
        }

        factory {
            WalletConnectMetadataRepository(
                metadataRepository = get()
            )
        }.bind(SessionRepository::class)

        factory {
            DappsListModel(
                uiSchedulers = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                sessionsRepository = get(),
                walletConnectServiceAPI = get()
            )
        }

        factory {
            SelectNetworkViewModel(
                coincore = get(),
                ethDataManager = get()
            )
        }
    }
}
