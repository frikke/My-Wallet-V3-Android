package com.blockchain.walletconnect.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.walletConnectV2FeatureFlag
import com.blockchain.walletconnect.data.EthWalletAddressProvider
import com.blockchain.walletconnect.data.SignRequestHandler
import com.blockchain.walletconnect.data.WalletConnectMetadataRepository
import com.blockchain.walletconnect.data.WalletConnectService
import com.blockchain.walletconnect.data.WalletConnectV2ServiceImpl
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletConnectV2UrlValidator
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

        scoped {
            WalletConnectV2ServiceImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }.apply {
            bind(WalletConnectV2Service::class)
            bind(WalletConnectV2UrlValidator::class)
        }

        factory {
            SignRequestHandler(
                accountProvider = get(),
            )
        }.apply {
            bind(EthRequestSign::class)
            bind(EthSendTransactionRequest::class)
        }

        factory {
            EthWalletAddressProvider(
                coincore = get(),
                ethDataManager = get()
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
                walletConnectServiceAPI = get(),
                walletConnectV2Service = get(),
                walletConnectV2FeatureFlag = get(walletConnectV2FeatureFlag)
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
