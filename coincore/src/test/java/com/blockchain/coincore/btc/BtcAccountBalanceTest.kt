package com.blockchain.coincore.btc

import com.blockchain.coincore.impl.AccountRefreshTrigger
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.bitcoin
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import io.mockk.coEvery
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class BtcAccountBalanceTest : CoincoreTestBase() {

    private val payloadDataManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val walletPrefs: WalletStatusPrefs = mock()
    private val refreshTrigger: AccountRefreshTrigger = mock()

    private val xpubs = (XPubs(listOf(XPub(ACCOUNT_XPUB, XPub.Format.LEGACY))))
    private val jsonAccount: Account = mock {
        on { isArchived }.thenReturn(false)
        on { xpubs }.thenReturn(xpubs)
        on { label }.thenReturn("label")
    }

    private val subject =
        BtcCryptoWalletAccount(
            payloadDataManager = payloadDataManager,
            hdAccountIndex = -1,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = jsonAccount,
            isHDAccount = true,
            walletPreferences = walletPrefs,
            refreshTrigger = refreshTrigger,
            addressResolver = mock()
        )

    @Before
    fun setup() {
        initMocks()
        whenever(exchangeRates.exchangeRateToUserFiat(CryptoCurrency.BTC))
            .thenReturn(Observable.just(BTC_TO_USER_RATE))
    }

    @Test
    fun `non zero balance calculated correctly`() {
        val btcBalance = 100.bitcoin()

        coEvery {
            unifiedBalancesService.balanceForWallet(
                subject,
                any()
            )
        } returns flowOf(
            DataResource.Data(
                NetworkBalance(
                    currency = subject.currency,
                    balance = btcBalance,
                    unconfirmedBalance = 0.bitcoin(),

                    exchangeRate = BTC_TO_USER_RATE
                )
            )
        )

        subject.balanceRx()
            .test().await()
            .assertValue {
                it.total == btcBalance &&
                    it.withdrawable == btcBalance &&
                    it.pending.isZero &&
                    it.exchangeRate == BTC_TO_USER_RATE
            }
    }

    @Test
    fun `zero balance calculated correctly`() {
        coEvery {
            unifiedBalancesService.balanceForWallet(
                subject,
                any()
            )
        } returns flowOf(
            DataResource.Data(
                NetworkBalance(
                    currency = subject.currency,
                    balance = 0.bitcoin(),
                    unconfirmedBalance = 0.bitcoin(),
                    exchangeRate = BTC_TO_USER_RATE
                )
            )
        )

        subject.balanceRx()
            .test().await()
            .assertValue {
                it.total.isZero &&
                    it.withdrawable.isZero &&
                    it.pending.isZero &&
                    it.exchangeRate == BTC_TO_USER_RATE
            }
    }

    companion object {
        private const val ACCOUNT_XPUB = "1234jfwepsdfapsksefksdwperoun894y98hefjbnakscdfoiw4rnwef"

        private val BTC_TO_USER_RATE = ExchangeRate(
            from = CryptoCurrency.BTC,
            to = TEST_USER_FIAT,
            rate = 38000.toBigDecimal()
        )
    }
}
