package com.blockchain.coincore.eth

import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import org.junit.Test
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import thepit.PitLinking

@Suppress("LocalVariableName", "SimplifyBooleanWithConstants")
class EthAddressParserTest : CoincoreTestBase() {
    private val payloadManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val assetCatalogue: Lazy<AssetCatalogue> = mock()
    private val custodialManager: CustodialWalletManager = mock()
    private val interestBalances: InterestBalanceDataManager = mock()
    private val tradingBalances: TradingBalanceDataManager = mock()
    private val walletPrefs: WalletStatus = mock()
    private val notificationUpdater: BackendNotificationUpdater = mock()
    private val labels: DefaultLabels = mock()
    private val pitLinking: PitLinking = mock()
    private val crashLogger: CrashLogger = mock()
    private val identity: UserIdentity = mock()
    private val features: InternalFeatureFlagApi = mock()
    private val formatUtils: FormatUtilities = mock()

    private val subject = EthAsset(
        payloadManager = payloadManager,
        ethDataManager = ethDataManager,
        feeDataManager = feeDataManager,
        assetCatalogue = assetCatalogue,
        custodialManager = custodialManager,
        interestBalances = interestBalances,
        tradingBalances = tradingBalances,
        exchangeRates = exchangeRates,
        currencyPrefs = currencyPrefs,
        walletPrefs = walletPrefs,
        notificationUpdater = notificationUpdater,
        labels = labels,
        pitLinking = pitLinking,
        crashLogger = crashLogger,
        identity = identity,
        features = features,
        formatUtils = formatUtils
    )

    @Test
    fun `raw address is parsed OK`() {
        val TEST_ADDRESS = ETH_ADDRESS

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    r.isContract == false &&
                    r.amount == null
            }
    }

    @Test
    fun `raw contract is parsed OK`() {
        val TEST_ADDRESS = ETH_ADDRESS

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(true))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    r.isContract == true &&
                    r.amount == null
            }
    }

    @Test
    fun `address with prefix is parsed OK`() {
        val TEST_ADDRESS = "ethereum:$ETH_ADDRESS"

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount == null
            }
    }

    @Test
    fun `address with prefix and amount is parsed OK, long form`() {
        val TEST_ADDRESS = "ethereum:$ETH_ADDRESS?value=2000000000000000000"
        val expectedValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, 2.toBigDecimal())

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount is CryptoValue &&
                    r.amount == expectedValue
            }
    }

    @Test
    fun `address with prefix and amount is parsed OK, exponential form`() {
        val TEST_ADDRESS = "ethereum:$ETH_ADDRESS?value=2.000e18"
        val expectedValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, 2.toBigDecimal())

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount is CryptoValue &&
                    r.amount == expectedValue
            }
    }

    @Test
    fun `address with no prefix and amount is parsed OK`() {
        val TEST_ADDRESS = "$ETH_ADDRESS?value=2.000e18"
        val expectedValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, 2.toBigDecimal())

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount is CryptoValue &&
                    r.amount == expectedValue
            }
    }

    @Test
    fun `address with prefix, amount and extra params is parsed OK, exponential form`() {
        val TEST_ADDRESS = "ethereum:$ETH_ADDRESS?dunno=eh&value=2.000e18&unknown=0.001"
        val expectedValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, 2.toBigDecimal())

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount is CryptoValue &&
                    r.amount == expectedValue
            }
    }

    @Test
    fun `address with prefix and unknown params is parsed OK`() {
        val TEST_ADDRESS = "$ETH_ADDRESS?unknown=2.000e18&dunno=0.988"

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(true)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertValue { result ->
                result is EthAddress
            }.assertValue { result ->
                val r = result as EthAddress
                r.address == ETH_ADDRESS &&
                    r.label == TEST_LABEL &&
                    !r.isContract &&
                    r.amount == null
            }
    }

    @Test
    fun `invalid address is rejected`() {
        val TEST_ADDRESS = ETH_ADDRESS

        whenever(formatUtils.isValidEthereumAddress(ETH_ADDRESS)).thenReturn(false)
        whenever(ethDataManager.isContractAddress(ETH_ADDRESS)).thenReturn(Single.just(false))

        subject.parseAddress(TEST_ADDRESS, TEST_LABEL)
            .test()
            .assertComplete()
            .assertNoValues()
    }

    companion object {
        private const val TEST_LABEL = "eth address label"
        private const val ETH_ADDRESS = "0xThisIsAValidEthAddress"
    }
}