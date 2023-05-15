@file:Suppress("SameParameterValue")

package com.blockchain.core.chains.bitcoincash

import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataRepository
import com.blockchain.testutils.rxInit
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.AccountV4
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class BchDataManagerTest {

    @get:Rule
    val rxSchedulers = rxInit {
        ioTrampoline()
    }

    private lateinit var subject: BchDataManager

    private val payloadDataManager: PayloadDataManager = mock()
    private var bchDataStore: BchDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val remoteLogger: RemoteLogger = mock()
    private val bitcoinApi: NonCustodialBitcoinService = mock()
    private val defaultLabels: DefaultLabels = mock {
        on { getDefaultNonCustodialWalletLabel() }.thenReturn(DEFAULT_LABEL)
    }
    private val metadataRepository: MetadataRepository = mock()

    @Before
    fun setUp() {
        subject = BchDataManager(
            payloadDataManager,
            bchDataStore,
            bitcoinApi,
            defaultLabels,
            mock(),
            metadataRepository,
            remoteLogger
        )
    }

    private fun mockAbsentMetadata() {
        whenever(metadataRepository.loadRawValue(any())).thenReturn(Maybe.empty())
    }

    private fun mockSingleMetadata(): String {
        val account = GenericMetadataAccount("account label")
        val metaData = GenericMetadataWallet(accounts = listOf()).addAccount(account)

        whenever(metadataRepository.loadRawValue(any())).thenReturn(
            Maybe.just(
                metaData.toJson()
            )
        )

        return metaData.toJson()
    }

    private fun mockRestoringSingleBchWallet(xpub: String): GenericMetadataWallet {
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(listOf(btcAccount))
        whenever(btcAccount.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub)

        return GenericMetadataWallet(
            accounts = listOf(
                GenericMetadataAccount(
                    _xpub = xpub,
                    label = "lalal"
                )
            )
        )
    }

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearAccountDetails()
        // Assert
        verify(bchDataStore).clearData()
        verifyNoMoreInteractions(bchDataStore)
    }

    @Test
    fun `initBchWallet create new metadata payload wo second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        mockAbsentMetadata()
        mockRestoringSingleBchWallet("xpub")

        whenever(bchDataStore.bchMetadata!!.toJson()).thenReturn("{}")
        whenever(metadataRepository.saveRawValue(any(), any())).thenReturn(Completable.complete())

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet retrieve existing data payload wo second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(defaultLabels.getDefaultNonCustodialWalletLabel()).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet create new metadata payload with second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)

        // Arrange
        mockAbsentMetadata()
        mockRestoringSingleBchWallet("xpub")

        whenever(bchDataStore.bchMetadata!!.toJson()).thenReturn("{}")
        whenever(metadataRepository.saveRawValue(any(), any())).thenReturn(Completable.complete())

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `initBchWallet retrieve existing data payload with second pw`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        mockSingleMetadata()
        mockRestoringSingleBchWallet("xpub")
        whenever(defaultLabels.getDefaultNonCustodialWalletLabel()).thenReturn("label")

        // Act
        val testObserver = subject.initBchWallet("Bitcoin cash account").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `fetchMetadata doesn't exist`() {
        // Arrange
        mockAbsentMetadata()

        // Act
        val testObserver = subject.fetchMetadata("label", 1).isEmpty.test()

        // Assert
        testObserver.assertValueAt(0, true)
        testObserver.assertComplete()
    }

    @Test
    fun `fetchMetadata exists`() {
        // Arrange
        val walletJson = mockSingleMetadata()

        // Act
        val testObserver = subject.fetchMetadata("label", 1).test()

        // Assert
        testObserver.assertComplete()
        Assert.assertEquals(walletJson, testObserver.values()[0].toJson())
    }

    @Test
    fun `restoreBchWallet with 2nd pw 1 account`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val xpub = "xpub"
        val metaData = mockRestoringSingleBchWallet(xpub)

        // Act
        val restoredWallet = subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub)
        assert(restoredWallet.accounts[0].xpubs() == XPubs(XPub(xpub, XPub.Format.LEGACY)))
    }

    @Test
    fun `restoreBchWallet with 2nd pw 2 account`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub1 = "xpub1"
        val xpub2 = "xpub2"
        val btcAccount1: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccount2: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(listOf(btcAccount1, btcAccount2))
        whenever(btcAccount1.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub1)
        whenever(btcAccount2.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub2)

        val metaData = GenericMetadataWallet(
            accounts = listOf(
                GenericMetadataAccount(label = "", _xpub = xpub1),
                GenericMetadataAccount(label = "aasdas", _xpub = xpub2)
            )
        )

        // Act
        subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub1)
        verify(bchDataStore.bchWallet)!!.addWatchOnlyAccount(xpub2)
        assert(metaData.accounts[0].xpubs() == XPubs(XPub(xpub1, XPub.Format.LEGACY)))
        assert(metaData.accounts[1].xpubs() == XPubs(XPub(xpub2, XPub.Format.LEGACY)))
    }

    @Test
    fun `restoreBchWallet no 2nd pw 1 account`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub = "xpub"
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(listOf(btcAccount))
        whenever(btcAccount.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub)

        val metaData = GenericMetadataWallet(accounts = listOf(GenericMetadataAccount(label = "12", _xpub = xpub)))
        // Act
        val restored = subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet)!!.addAccount()
        assert(restored.accounts[0].xpubs() == XPubs(XPub(xpub, XPub.Format.LEGACY)))
    }

    @Test
    fun `restoreBchWallet no 2nd pw 2 account`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        val mnemonic = split("all all all all all all all all all all all all")
        whenever(payloadDataManager.mnemonic).thenReturn(mnemonic)

        // 1 account
        val xpub1 = "xpub1"
        val xpub2 = "xpub2"
        val btcAccount1: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccount2: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.accounts).thenReturn(mutableListOf(btcAccount1, btcAccount2))
        whenever(btcAccount1.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub1)
        whenever(btcAccount2.xpubForDerivation(Derivation.LEGACY_TYPE)).thenReturn(xpub2)
        val bchMetaDataAccount1 = GenericMetadataAccount("label 1")
        val bchMetaDataAccount2 = GenericMetadataAccount("label 2")
        val metaData = GenericMetadataWallet(accounts = listOf(bchMetaDataAccount1, bchMetaDataAccount2))

        // Act
        val newMetadata = subject.restoreBchWallet(metaData)

        // Assert
        verify(bchDataStore.bchWallet, times(2))!!.addAccount()
        assert(newMetadata.accounts[0].xpubs() == XPubs(XPub(xpub1, XPub.Format.LEGACY)))
        assert(newMetadata.accounts[1].xpubs() == XPubs(XPub(xpub2, XPub.Format.LEGACY)))
    }

    @Test
    fun `correctBtcOffsetIfNeed btc equal to bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        subject.correctBtcOffsetIfNeed()

        // Assert
        verify(payloadDataManager, never()).addAccountWithLabel(any())
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc more than bch account size`() {
        // Arrange
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val btcAccounts = mutableListOf(btcAccount, btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)

        // Act
        subject.correctBtcOffsetIfNeed()

        // Assert
        verify(payloadDataManager, never()).addAccountWithLabel(any())
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc 1 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 1
        val mockCallCount = 1
        val xpub = XPub(address = "xpub 2", XPub.Format.LEGACY)
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { xpubs }.thenReturn(XPubs(listOf(xpub)))
            on { xpubForDerivation(Derivation.LEGACY_TYPE) }.thenReturn(xpub.address)
        }

        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(bchAccount, bchAccount)
        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)
        val newBtcAccount = mock<AccountV4> {
            on { xpubForDerivation("legacy") }.thenReturn("213123")
        }
        whenever(payloadDataManager.addAccountWithLabel("BTC label 2")).thenReturn(Single.just(newBtcAccount))

        whenever(defaultLabels.getDefaultNonCustodialWalletLabel()).thenReturn("BTC label")

        // Act
        subject.correctBtcOffsetIfNeed().test()

        // Assert
        verify(payloadDataManager).addAccountWithLabel(any())
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts

        verify(bchDataStore.bchMetadata, times(btcAccountsNeeded + mockCallCount - 1))!!
            .updateXpubForAccountIndex(
                any(),
                any()
            )

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `correctBtcOffsetIfNeed btc 5 less than bch account size`() {
        // Arrange
        val btcAccountsNeeded = 5
        val mockCallCount = 1
        val xpub = XPub(address = "xpub 2", XPub.Format.LEGACY)
        val btcAccount: Account = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { xpubs }.thenReturn(XPubs(listOf(XPub(address = "xpub 2", XPub.Format.LEGACY))))
            on { xpubForDerivation(Derivation.LEGACY_TYPE) }.thenReturn(xpub.address)
        }

        val btcAccounts = mutableListOf(btcAccount)
        whenever(payloadDataManager.accounts).thenReturn(btcAccounts)

        val bchAccount: GenericMetadataAccount = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val bchAccounts = mutableListOf(
            bchAccount,
            bchAccount,
            bchAccount,
            bchAccount,
            bchAccount,
            bchAccount
        )
        val mockAccounts = listOf<Account>(
            mock {
                on { xpubForDerivation("legacy") }.thenReturn("xpudfsasdfasb")
            },
            mock {
                on { xpubForDerivation("legacy") }.thenReturn("xpubfsagdasf")
            },
            mock {
                on { xpubForDerivation("legacy") }.thenReturn("xpubwrqsa")
            },
            mock {
                on { xpubForDerivation("legacy") }.thenReturn("xpubasdgasdgas")
            },
            mock {
                on { xpubForDerivation("legacy") }.thenReturn("xpubasdgaserqwedgas")
            }
        )
        val offset = btcAccounts.size + 1
        mockAccounts.forEachIndexed { index, account ->
            whenever(payloadDataManager.addAccountWithLabel("$DEFAULT_LABEL ${index + offset}"))
                .thenReturn(
                    Single.just(account)
                )
        }

        whenever(bchDataStore.bchMetadata?.accounts).thenReturn(bchAccounts)
        // Act
        subject.correctBtcOffsetIfNeed().test()

        // Assert
        verify(payloadDataManager, times(bchAccounts.size - btcAccounts.size)).addAccountWithLabel(any())
        verify(payloadDataManager, atLeastOnce()).accounts
        verify(bchDataStore.bchMetadata)!!.accounts
        verify(
            bchDataStore.bchMetadata,
            times(btcAccountsNeeded + mockCallCount - 1)
        )!!.updateXpubForAccountIndex(
            any(),
            any()
        )

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(bchDataStore.bchMetadata)
    }

    @Test
    fun `get balance`() {
        val xpub = XPub("address", XPub.Format.LEGACY)
        val xpubs = XPubs(xpub)
        val map = mapOf(
            xpub.address to Balance(
                finalBalance = BigInteger.TEN,
                totalReceived = BigInteger.ZERO
            )
        )
        whenever(bchDataStore.bchMetadata).thenReturn(
            GenericMetadataWallet(
                accounts = listOf(
                    GenericMetadataAccount(label = "", _xpub = "address")
                )
            )
        )

        BchDataManager(
            payloadDataManager = mock {
                on { getBalanceOfBchAccounts(listOf(xpubs)) }.thenReturn(Observable.just(map))
            },
            bchDataStore = bchDataStore,
            bitcoinApi = mock(),
            defaultLabels = defaultLabels,
            metadataRepository = mock(),
            bchBalanceCache = mock {
                on { getBalanceOfAddresses(listOf(xpubs)) }.thenReturn(Single.just(map))
            },
            remoteLogger = mock()
        ).getBalance(xpubs)
            .test()
            .assertNoErrors()
            .assertValue(BigInteger.TEN)
    }

    private fun split(words: String): List<String> {
        return words.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
    }
}

private const val DEFAULT_LABEL = "account"
