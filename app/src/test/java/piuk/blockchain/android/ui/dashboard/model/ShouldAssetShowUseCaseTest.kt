package piuk.blockchain.android.ui.dashboard.model

import app.cash.turbine.test
import com.blockchain.coincore.AccountBalance
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.coEvery
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
            assetDisplayBalanceFF = assetDisplayBalanceFF,
            localSettingsPrefs = localSettingsPrefs,
            watchlistService = watchlistService
        )
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
            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
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

            verify { localSettingsPrefs.hideSmallBalancesEnabled }
            verify {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            }
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

            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
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

            verify {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            }
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

            every { localSettingsPrefs.hideSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
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

            verify {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            }
            verify { accountBalance.totalFiat }
        }
}
