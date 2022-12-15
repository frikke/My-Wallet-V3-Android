package piuk.blockchain.android.ui.brokerage.sell

import app.cash.turbine.test
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellService
import com.blockchain.data.DataResource
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.AccountsSorting

@OptIn(ExperimentalCoroutinesApi::class)
class SellViewModelTest {

    private val sellService: SellService = mockk()
    private val coincore: Coincore = mockk()
    private val accountSorting: AccountsSorting = mockk()
    private val localSettingsPrefs: LocalSettingsPrefs = mockk()
    private val hideDustFF: FeatureFlag = mockk()
    private lateinit var subject: SellViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        subject = SellViewModel(
            sellService = sellService,
            coincore = coincore,
            accountsSorting = accountSorting,
            localSettingsPrefs = localSettingsPrefs,
            hideDustFlag = hideDustFF
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given user is eligible when sell eligibility is requested then the model is updated`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<SellEligibility>>()
        every { sellService.loadSellAssets() }.returns(dataResource)

        subject.viewState.test {
            expectMostRecentItem()
            subject.viewCreated(ModelConfigArgs.NoArgs)

            dataResource.emit(DataResource.Loading)
            awaitItem().run {
                showLoader shouldBe false
            }

            val data = DataResource.Data(SellEligibility.Eligible(listOf(mockk(), mockk(), mockk())))
            dataResource.emit(data)

            awaitItem().run {
                showLoader shouldBe false
                sellEligibility shouldBeEqualTo data
            }
        }

        verify { sellService.loadSellAssets() }
        coVerify(exactly = 0) { hideDustFF.coEnabled() }
        verify(exactly = 0) { localSettingsPrefs.hideSmallBalancesEnabled }
        verify(exactly = 0) { accountSorting.sorter() }
        verify(exactly = 0) {
            coincore.walletsWithActions(
                actions = setOf(AssetAction.Sell),
                filter = AssetFilter.All,
                sorter = accountSorting.sorter()
            )
        }
    }

    @Test
    fun `given feature flag off when supported accounts load then dust balance accounts are returned`() = runTest {
        coEvery { hideDustFF.coEnabled() }.returns(false)

        val assetInfo: AssetInfo = mockk()
        val assetInfo1: AssetInfo = mockk()

        val noDustFiat: Money = mockk {
            every { isDust() }.returns(false)
        }
        val noDustBalance: AccountBalance = mockk {
            every { totalFiat }.returns(noDustFiat)
        }

        val dustFiat: Money = mockk {
            every { isDust() }.returns(true)
        }
        val dustBalance: AccountBalance = mockk {
            every { totalFiat }.returns(dustFiat)
        }
        val accountsList: SingleAccountList = listOf(
            mockk<CryptoAccount> {
                every { currency }.returns(assetInfo)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            },
            mockk<CryptoAccount> {
                every { currency }.returns(assetInfo1)
                every { balanceRx }.returns(Observable.just(dustBalance))
            },
        )

        every {
            coincore.walletsWithActions(
                actions = setOf(AssetAction.Sell),
                sorter = accountSorting.sorter()
            )
        }.returns(Single.just(accountsList))

        val supportedAssets: List<AssetInfo> = listOf(assetInfo, assetInfo1)

        subject.viewState.test {
            expectMostRecentItem()

            subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))

            awaitItem().run {
                supportedAccountList shouldBeEqualTo DataResource.Data(accountsList)
            }
        }

        verify(exactly = 0) { sellService.loadSellAssets() }
        coVerify(exactly = 1) { hideDustFF.coEnabled() }
        verify(exactly = 0) { localSettingsPrefs.hideSmallBalancesEnabled }
        verify(exactly = 1) { accountSorting.sorter() }
        verify(exactly = 1) {
            coincore.walletsWithActions(
                actions = setOf(AssetAction.Sell),
                sorter = accountSorting.sorter()
            )
        }
    }

    @Test
    fun `given feature flag on and local setting off when supported accounts load then dust balance accounts are returned`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(false)

            val assetInfo: AssetInfo = mockk()
            val assetInfo1: AssetInfo = mockk()

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val dustFiat: Money = mockk {
                every { isDust() }.returns(true)
            }
            val dustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(dustFiat)
            }
            val accountsList: SingleAccountList = listOf(
                mockk<CryptoAccount> {
                    every { currency }.returns(assetInfo)
                    every { balanceRx }.returns(Observable.just(noDustBalance))
                },
                mockk<CryptoAccount> {
                    every { currency }.returns(assetInfo1)
                    every { balanceRx }.returns(Observable.just(dustBalance))
                }
            )

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(assetInfo, assetInfo1)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))

                awaitItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(accountsList)
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }

    @Test
    fun `given feature flag on and local setting on when supported accounts load then dust balance accounts aren't returned`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)

            val assetInfo: AssetInfo = mockk()
            val assetInfo1: AssetInfo = mockk()

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val dustFiat: Money = mockk {
                every { isDust() }.returns(true)
            }
            val dustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(dustFiat)
            }
            val noDustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val dustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo1)
                every { balanceRx }.returns(Observable.just(dustBalance))
            }
            val accountsList: SingleAccountList = listOf(noDustAccount, dustAccount)

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(assetInfo, assetInfo1)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))

                awaitItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(listOf(noDustAccount))
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }

    @Test
    fun `given accounts returned when sell pairs don't contain the asset then account is filtered out from list`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)

            val assetInfo: AssetInfo = mockk()
            val assetInfo1: AssetInfo = mockk()
            val assetInfo2: AssetInfo = mockk()

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val dustFiat: Money = mockk {
                every { isDust() }.returns(true)
            }
            val dustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(dustFiat)
            }
            val noDustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val dustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo1)
                every { balanceRx }.returns(Observable.just(dustBalance))
            }
            val unsupportedSellAssetAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo2)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val accountsList: SingleAccountList = listOf(noDustAccount, dustAccount, unsupportedSellAssetAccount)

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(assetInfo, assetInfo1)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))

                awaitItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(listOf(noDustAccount))
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }

    @Test
    fun `given accounts returned and setting off when sell pairs don't contain the asset then account is filtered out from list`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(false)

            val assetInfo: AssetInfo = mockk()
            val assetInfo1: AssetInfo = mockk()
            val assetInfo2: AssetInfo = mockk()

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val dustFiat: Money = mockk {
                every { isDust() }.returns(true)
            }
            val dustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(dustFiat)
            }
            val noDustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val dustAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo1)
                every { balanceRx }.returns(Observable.just(dustBalance))
            }
            val unsupportedSellAssetAccount = mockk<CryptoAccount> {
                every { currency }.returns(assetInfo2)
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val accountsList: SingleAccountList = listOf(noDustAccount, dustAccount, unsupportedSellAssetAccount)

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(assetInfo, assetInfo1)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))

                awaitItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(listOf(noDustAccount, dustAccount))
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }

    @Test
    fun `given search term updated when term is found in ticker then list is filtered to matching results`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)

            val btcAsset: AssetInfo = mockk {
                every { networkTicker }.returns("BTC")
                every { name }.returns("Bitcoin")
            }
            val ethAsset: AssetInfo = mockk {
                every { networkTicker }.returns("ETH")
                every { name }.returns("Ethereum")
            }

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val btcAccount = mockk<CryptoAccount> {
                every { currency }.returns(btcAsset)
                every { label }.returns("Trading Account")
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val ethAccount = mockk<CryptoAccount> {
                every { currency }.returns(ethAsset)
                every { label }.returns("Trading Account")
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }

            val accountsList: SingleAccountList = listOf(btcAccount, ethAccount)

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(btcAsset, ethAsset)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))
                subject.onIntent(SellIntent.FilterAccounts("eth"))

                expectMostRecentItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(listOf(ethAccount))
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }

    @Test
    fun `given search term updated when term is not found then empty list is returned`() =
        runTest {
            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)

            val btcAsset: AssetInfo = mockk {
                every { networkTicker }.returns("BTC")
                every { name }.returns("Bitcoin")
            }
            val ethAsset: AssetInfo = mockk {
                every { networkTicker }.returns("ETH")
                every { name }.returns("Ethereum")
            }

            val noDustFiat: Money = mockk {
                every { isDust() }.returns(false)
            }
            val noDustBalance: AccountBalance = mockk {
                every { totalFiat }.returns(noDustFiat)
            }

            val btcAccount = mockk<CryptoAccount> {
                every { currency }.returns(btcAsset)
                every { label }.returns("Trading Account")
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }
            val ethAccount = mockk<CryptoAccount> {
                every { currency }.returns(ethAsset)
                every { label }.returns("Trading Account")
                every { balanceRx }.returns(Observable.just(noDustBalance))
            }

            val accountsList: SingleAccountList = listOf(btcAccount, ethAccount)

            every {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }.returns(Single.just(accountsList))

            val supportedAssets: List<AssetInfo> = listOf(btcAsset, ethAsset)

            subject.viewState.test {
                expectMostRecentItem()

                subject.onIntent(SellIntent.LoadSupportedAccounts(supportedAssets))
                subject.onIntent(SellIntent.FilterAccounts("ttt"))

                expectMostRecentItem().run {
                    supportedAccountList shouldBeEqualTo DataResource.Data(emptyList())
                }
            }

            verify(exactly = 0) { sellService.loadSellAssets() }
            coVerify(exactly = 1) { hideDustFF.coEnabled() }
            verify(exactly = 1) { localSettingsPrefs.hideSmallBalancesEnabled }
            verify(exactly = 1) { accountSorting.sorter() }
            verify(exactly = 1) {
                coincore.walletsWithActions(
                    actions = setOf(AssetAction.Sell),
                    sorter = accountSorting.sorter()
                )
            }
        }
}
