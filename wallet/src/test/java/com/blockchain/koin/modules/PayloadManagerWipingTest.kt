package com.blockchain.koin.modules

import com.blockchain.AppVersion
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.koin.payloadScope
import com.blockchain.koin.walletModule
import com.blockchain.logging.RemoteLogger
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.Device
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadScopeWiper
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.After
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class PayloadManagerWipingTest : KoinTest {

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `After wiping the payload manager, a new request for a payload manager gets a distinct instance`() {
        startKoin {
            modules(
                listOf(
                    walletModule,
                    module {
                        single { mock<WalletApi>() }
                        single { mock<RemoteLogger>() }
                        single { mock<NonCustodialBitcoinService>() }
                        single { mock<Device>() }
                        single { mock<AppVersion>() }
                    }
                )
            )
        }

        val firstPayloadManager: PayloadManager = payloadScope.get()
        val secondPayloadManager: PayloadManager = payloadScope.get()

        firstPayloadManager `should be` secondPayloadManager

        val wiper: PayloadScopeWiper = payloadScope.get()

        wiper.wipe()

        val thirdPayloadManager: PayloadManager by payloadScope.inject()

        thirdPayloadManager `should not be` secondPayloadManager
    }
}
