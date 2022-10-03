package com.blockchain.coincore.fiat

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class FiatAssetTransferTest : KoinTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val labels: DefaultLabels = mock()
    private val exchangeRateDataManager: ExchangeRatesDataManager = mock()
    private val traService: TradingService = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val bankService: BankService = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            listOf(
                module {
                    factory {
                        labels
                    }
                    factory {
                        exchangeRateDataManager
                    }
                    factory {
                        traService
                    }
                    factory {
                        custodialWalletManager
                    }
                    factory {
                        bankService
                    }
                }
            )
        )
    }
    private val subject = FiatAsset(
        SELECTED_FIAT
    )

    @Test
    fun transferListForCustodialSource() {

        whenever(labels.getDefaultCustodialFiatWalletLabel(any())).thenReturn(DEFAULT_LABEL)

        val sourceAccount: CustodialTradingAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { list ->
                list.isEmpty()
            }
    }

    @Test
    fun transferListForInterestSource() {
        val sourceAccount: CryptoInterestAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    @Test
    fun transferListForNonCustodialSource() {
        val sourceAccount: CryptoNonCustodialAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    @Test
    fun transferListForFiatSource() {
        val sourceAccount: FiatCustodialAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    companion object {
        private const val DEFAULT_LABEL = "label"
        private val SELECTED_FIAT = USD
    }
}
