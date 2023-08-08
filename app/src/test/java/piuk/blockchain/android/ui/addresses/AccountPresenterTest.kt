package piuk.blockchain.android.ui.addresses

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AddressAnalytics
import com.blockchain.analytics.events.WalletAnalytics
import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.MultipleWalletsAsset
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.withSettings
import piuk.blockchain.android.R

class AccountPresenterTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val activity: AccountView = mock()
    private val privateKeyFactory: PrivateKeyFactory = mock()

    private val mockedBtcCryptoAsset: CryptoAsset =
        Mockito.mock(CryptoAsset::class.java, withSettings().extraInterfaces(MultipleWalletsAsset::class.java))

    private val mockedBchCryptoAsset: CryptoAsset =
        Mockito.mock(CryptoAsset::class.java, withSettings().extraInterfaces(MultipleWalletsAsset::class.java))

    private val coincore: Coincore = mock {
        on { get(CryptoCurrency.BTC) }.thenReturn(mockedBtcCryptoAsset)
        on { get(CryptoCurrency.BCH) }.thenReturn(mockedBchCryptoAsset)
    }

    private val analytics: Analytics = mock()

    private val subject: AccountPresenter = AccountPresenter(
        privateKeyFactory,
        coincore,
        analytics
    )

    @Before
    fun setUp() {
        subject.attachView(activity)
    }

    @Test
    fun createNewAccountLabelExists() {
        // Arrange
        whenever(coincore.isLabelUnique(any())).thenReturn(Single.just(false))

        // Act
        subject.createNewAccount(NEW_BTC_LABEL)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showError(com.blockchain.stringResources.R.string.label_name_match)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun createNewAccountSuccessful() {
        // Arrange
        val newAccount: BtcCryptoWalletAccount = mock {
            on { xpubAddress }.thenReturn(VALID_XPUB)
        }
        whenever((mockedBtcCryptoAsset as MultipleWalletsAsset).createWalletFromLabel(NEW_BTC_LABEL, null)).thenReturn(
            Single.just(newAccount)
        )
        whenever((mockedBchCryptoAsset as MultipleWalletsAsset).createWalletFromAddress(VALID_XPUB)).thenReturn(
            Completable.complete()
        )

        whenever(coincore.isLabelUnique(any())).thenReturn(Single.just(true))

        // Act
        subject.createNewAccount(NEW_BTC_LABEL)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showSuccess(com.blockchain.stringResources.R.string.remote_save_ok)
        verifyNoMoreInteractions(activity)
        verify(analytics).logEvent(WalletAnalytics.AddNewWallet)
    }

    @Test
    fun createNewAccountIncorrectSecondPassword() {
        // Arrange
        whenever((mockedBtcCryptoAsset as MultipleWalletsAsset).createWalletFromLabel(NEW_BTC_LABEL, null))
            .thenReturn(Single.error(DecryptionException()))

        whenever(coincore.isLabelUnique(any())).thenReturn(Single.just(true))

        // Act
        subject.createNewAccount(NEW_BTC_LABEL)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showError(com.blockchain.stringResources.R.string.double_encryption_password_error)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun createNewAccountUnknownException() {
        // Arrange
        whenever((mockedBtcCryptoAsset as MultipleWalletsAsset).createWalletFromLabel(NEW_BTC_LABEL, null))
            .thenReturn(Single.error(RuntimeException()))

        whenever(coincore.isLabelUnique(any())).thenReturn(Single.just(true))

        // Act
        subject.createNewAccount(NEW_BTC_LABEL)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showError(com.blockchain.stringResources.R.string.unexpected_error)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateImportedAddressLabelSuccessful() {
        // Arrange
        val cryptoValue = mock<AccountBalance> {
            on { total }.thenReturn(CryptoValue.fromMinor(CryptoCurrency.BTC, 1.toBigInteger()))
            on { withdrawable }.thenReturn(CryptoValue.fromMinor(CryptoCurrency.BTC, 1.toBigInteger()))
        }
        val importedAccount: BtcCryptoWalletAccount = mock {
            on { updateLabel(UPDATED_BTC_LABEL) }.thenReturn(Completable.complete())
            on { balanceRx() }.thenReturn(Observable.just(cryptoValue))
        }

        // Act
        subject.updateImportedAddressLabel(UPDATED_BTC_LABEL, importedAccount)

        // Assert
        verify(activity).showSuccess(com.blockchain.stringResources.R.string.remote_save_ok)
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
    }

    @Test
    fun updateImportedAddressLabelFailed() {
        // Arrange
        val importedAccount: BtcCryptoWalletAccount = mock {
            on { updateLabel(UPDATED_BTC_LABEL) }.thenReturn(Completable.error(RuntimeException()))
        }

        // Act
        subject.updateImportedAddressLabel(UPDATED_BTC_LABEL, importedAccount)

        // Assert
        verify(activity).showError(com.blockchain.stringResources.R.string.remote_save_failed)
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
    }

    @Test
    fun importedAddressHasBalance() {
        // Arrange
        val sendingAccount: BtcCryptoWalletAccount = mock()
        val balance: AccountBalance = mock {
            on { withdrawable }.thenReturn(CryptoValue.fromMinor(CryptoCurrency.BTC, 1.toBigInteger()))
        }
        whenever(sendingAccount.balanceRx()).thenReturn(Observable.just(balance))

        // Act
        subject.checkBalanceForTransfer(sendingAccount)

        // Assert
        verify(activity).showTransferFunds(sendingAccount)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun importedAddressHasNoBalance() {
        // Arrange
        val sendingAccount: BtcCryptoWalletAccount = mock()
        val balance: AccountBalance = mock {
            on { withdrawable }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        }
        whenever(sendingAccount.balanceRx()).thenReturn(Observable.just(balance))

        // Act
        subject.checkBalanceForTransfer(sendingAccount)

        // Assert
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun bip38requiredPassword() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS)).thenReturn(PrivateKeyFactory.BIP38)

        // Act
        val result = subject.importRequiresPassword(SCANNED_ADDRESS)

        // Assert
        assertTrue(result)
    }

    fun `public Key Doesn't Require Password`() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS)).thenReturn(null)

        // Act
        val result = subject.importRequiresPassword(SCANNED_ADDRESS)

        // Assert
        assertFalse(result)
    }

    fun `non Bip38 Doesn't Require Password`() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS)).thenReturn(PrivateKeyFactory.HEX)

        // Act
        val result = subject.importRequiresPassword(SCANNED_ADDRESS)

        // Assert
        assertFalse(result)
    }

    @Test
    fun importBip38AddressWithValidPassword() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS))
            .thenReturn(PrivateKeyFactory.BIP38)

        val importedAccount: BtcCryptoWalletAccount = mock()
        whenever(
            (mockedBtcCryptoAsset as MultipleWalletsAsset).importWalletFromKey(
                SCANNED_ADDRESS,
                PrivateKeyFactory.BIP38,
                BIP38_PASSWORD,
                null
            )
        ).thenReturn(Single.just(importedAccount))

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, BIP38_PASSWORD, null)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showSuccess(com.blockchain.stringResources.R.string.private_key_successfully_imported)
        verify(activity).showRenameImportedAddressDialog(importedAccount)
        verifyNoMoreInteractions(activity)
        verify(analytics).logEvent(AddressAnalytics.ImportBTCAddress)
    }

    @Test
    fun importBip38AddressWithIncorrectPassword() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS))
            .thenReturn(PrivateKeyFactory.BIP38)

        whenever(
            (mockedBtcCryptoAsset as MultipleWalletsAsset).importWalletFromKey(
                SCANNED_ADDRESS,
                PrivateKeyFactory.BIP38,
                BIP38_PASSWORD,
                null
            )
        ).thenReturn(Single.error(RuntimeException()))

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, BIP38_PASSWORD, null)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showError(com.blockchain.stringResources.R.string.no_private_key)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedNonBip38() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS))
            .thenReturn(PrivateKeyFactory.HEX)

        val importedAccount: BtcCryptoWalletAccount = mock()
        whenever(
            (mockedBtcCryptoAsset as MultipleWalletsAsset).importWalletFromKey(
                SCANNED_ADDRESS,
                PrivateKeyFactory.HEX,
                null,
                null
            )
        ).thenReturn(Single.just(importedAccount))

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, null)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showSuccess(com.blockchain.stringResources.R.string.private_key_successfully_imported)
        verify(activity).showRenameImportedAddressDialog(importedAccount)
        verifyNoMoreInteractions(activity)
        verify(analytics).logEvent(AddressAnalytics.ImportBTCAddress)
    }

    @Test
    fun onAddressScannedNonBip38Failure() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS))
            .thenReturn(PrivateKeyFactory.HEX)

        whenever(
            (mockedBtcCryptoAsset as MultipleWalletsAsset).importWalletFromKey(
                SCANNED_ADDRESS,
                PrivateKeyFactory.HEX,
                null,
                null
            )
        ).thenReturn(Single.error(RuntimeException()))

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, null)

        // Assert
        verify(activity).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(activity).dismissProgressDialog()
        verify(activity).showError(com.blockchain.stringResources.R.string.no_private_key)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedWatchOnlyInvalidAddress() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS)).thenReturn(null)
        whenever(mockedBtcCryptoAsset.isValidAddress(SCANNED_ADDRESS)).thenReturn(true)

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, null)

        // Assert
        verify(activity).showError(com.blockchain.stringResources.R.string.watch_only_not_supported)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedUnknownFormat() {
        // Arrange
        whenever(privateKeyFactory.getFormat(SCANNED_ADDRESS)).thenReturn(null)
        whenever(mockedBtcCryptoAsset.isValidAddress(SCANNED_ADDRESS)).thenReturn(false)

        // Act
        subject.importScannedAddress(SCANNED_ADDRESS, null)

        // Assert
        verify(activity).showError(com.blockchain.stringResources.R.string.privkey_error)
        verifyNoMoreInteractions(activity)
    }

    companion object {
        private const val VALID_XPUB = "hsdjseoihefsihdfsihefsohifes"
        private const val NEW_BTC_LABEL = "New BTC Account"
        private const val UPDATED_BTC_LABEL = "Updated Label"
        private const val SCANNED_ADDRESS = "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS"
        private const val BIP38_PASSWORD = "verysecurepassword"
    }
}
