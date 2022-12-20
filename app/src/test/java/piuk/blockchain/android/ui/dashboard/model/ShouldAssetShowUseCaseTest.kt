package piuk.blockchain.android.ui.dashboard.model

import app.cash.turbine.test
import com.blockchain.coincore.AccountBalance
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldAssetShowUseCaseTest {

    private lateinit var subject: ShouldAssetShowUseCase

    private val hideDustFF: FeatureFlag = mockk()
    private val assetDisplayBalanceFF: FeatureFlag = mockk() {
        coEvery { coEnabled() } returns false
    }
    private val localSettingsPrefs: LocalSettingsPrefs = mockk()
    private val watchlistService: WatchlistService = mockk()

    private val currency = object : CryptoCurrency(
        displayTicker = "NOPE",
        networkTicker = "NOPE",
        name = "Not a real thing",
        categories = setOf(AssetCategory.CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    @Before
    fun setup() {
        subject = ShouldAssetShowUseCase(
            hideDustFeatureFlag = hideDustFF,
            assetDisplayBalanceFF = assetDisplayBalanceFF,
            localSettingsPrefs = localSettingsPrefs,
            watchlistService = watchlistService
        )
    }

    @Test
    fun `given hide dust flag is disabled then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        coEvery { hideDustFF.coEnabled() }.returns(false)
        every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(false)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }.returns(
            flowOf(
                DataResource.Data(false)
            )
        )

        subject.invoke(accountBalance).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        coVerify { hideDustFF.coEnabled() }
        verify(exactly = 0) { localSettingsPrefs.hideSmallBalancesEnabled }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
        verify { accountBalance.totalFiat wasNot Called }
    }

    @Test
    fun `given hide dust ff on & local pref off then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        coEvery { hideDustFF.coEnabled() }.returns(true)
        every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(false)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }.returns(
            flowOf(
                DataResource.Data(false)
            )
        )

        subject.invoke(accountBalance).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        coVerify { hideDustFF.coEnabled() }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
        verify { accountBalance.totalFiat wasNot Called }
    }

    @Test
    fun `given both flag and pref are on and asset is in watchlist then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        coEvery { hideDustFF.coEnabled() }.returns(true)
        every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }.returns(
            flowOf(
                DataResource.Data(true)
            )
        )

        subject.invoke(accountBalance).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        coVerify { hideDustFF.coEnabled() }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
        verify { accountBalance.totalFiat wasNot Called }
    }

    @Test
    fun `given both flag and pref are on and dust balance asset not in watchlist then asset shouldn't show`() =
        runTest {
            val dustBalance: Money = mockk {
                every { isDust() }.returns(true)
            }
            val accountBalance: AccountBalance = mockk {
                every { total }.returns(Money.zero(currency))
                every { totalFiat }.returns(dustBalance)
            }

            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = false)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(accountBalance).test {
                with(expectMostRecentItem()) {
                    assertEquals(false, this)
                }
            }

            coVerify { hideDustFF.coEnabled() }
            verify { localSettingsPrefs.hideSmallBalancesEnabled }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
            verify { accountBalance.totalFiat }
            verify { dustBalance.isDust() }
        }

    @Test
    fun `given both flag and pref are on and large balance asset not in watchlist then asset should show`() =
        runTest {
            val dustBalance: Money = mockk {
                every { isDust() }.returns(false)
            }
            val accountBalance: AccountBalance = mockk {
                every { total }.returns(Money.zero(currency))
                every { totalFiat }.returns(dustBalance)
            }

            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = false)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(accountBalance).test {
                with(expectMostRecentItem()) {
                    assertEquals(true, this)
                }
            }

            coVerify { hideDustFF.coEnabled() }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
            verify { accountBalance.totalFiat }
            verify { dustBalance.isDust() }
        }

    @Test
    fun `given both flag and pref are on and zero balance asset not in watchlist then asset should show`() =
        runTest {
            val accountBalance: AccountBalance = mockk {
                every { total }.returns(Money.zero(currency))
                every { totalFiat }.returns(Money.zero(currency))
            }

            coEvery { hideDustFF.coEnabled() }.returns(true)
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = false)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(accountBalance).test {
                with(expectMostRecentItem()) {
                    assertEquals(true, this)
                }
            }

            coVerify { hideDustFF.coEnabled() }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = false)) }
            verify { accountBalance.totalFiat }
        }
}
