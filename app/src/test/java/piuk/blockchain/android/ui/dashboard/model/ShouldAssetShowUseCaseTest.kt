package piuk.blockchain.android.ui.dashboard.model

import app.cash.turbine.test
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
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
            localSettingsPrefs = localSettingsPrefs,
            watchlistService = watchlistService
        )
    }

    @Test
    fun `given hide dust flag is disabled then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        val account: BlockchainAccount = mockk {
            every { balance }.returns(flowOf(accountBalance))
        }

        every { hideDustFF.enabled }.returns(Single.just(false))
        every { localSettingsPrefs.areSmallBalancesEnabled }.returns(false)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }.returns(
            flowOf(
                DataResource.Data(false)
            )
        )

        subject.invoke(currency, account).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        verify { hideDustFF.enabled }
        verify { localSettingsPrefs.areSmallBalancesEnabled }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
        verify { account.balance }
        verify { accountBalance.totalFiat wasNot Called }
    }

    @Test
    fun `given hide dust ff on & local pref off then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        val account: BlockchainAccount = mockk {
            every { balance }.returns(flowOf(accountBalance))
        }

        every { hideDustFF.enabled }.returns(Single.just(true))
        every { localSettingsPrefs.areSmallBalancesEnabled }.returns(false)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }.returns(
            flowOf(
                DataResource.Data(false)
            )
        )

        subject.invoke(currency, account).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        verify { hideDustFF.enabled }
        verify { localSettingsPrefs.areSmallBalancesEnabled }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
        verify { account.balance }
        verify { accountBalance.totalFiat wasNot Called }
    }

    @Test
    fun `given both flag and pref are on and asset is in watchlist then asset should show`() = runTest {
        val accountBalance: AccountBalance = mockk {
            every { total }.returns(Money.zero(currency))
        }

        val account: BlockchainAccount = mockk {
            every { balance }.returns(flowOf(accountBalance))
        }

        every { hideDustFF.enabled }.returns(Single.just(true))
        every { localSettingsPrefs.areSmallBalancesEnabled }.returns(true)
        every { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }.returns(
            flowOf(
                DataResource.Data(true)
            )
        )

        subject.invoke(currency, account).test {
            with(expectMostRecentItem()) {
                assertEquals(true, this)
            }
        }

        verify { hideDustFF.enabled }
        verify { localSettingsPrefs.areSmallBalancesEnabled }
        verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
        verify { account.balance }
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

            val account: BlockchainAccount = mockk {
                every { balance }.returns(flowOf(accountBalance))
            }

            every { hideDustFF.enabled }.returns(Single.just(true))
            every { localSettingsPrefs.areSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = true)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(currency, account).test {
                with(expectMostRecentItem()) {
                    assertEquals(false, this)
                }
            }

            verify { hideDustFF.enabled }
            verify { localSettingsPrefs.areSmallBalancesEnabled }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
            verify { account.balance }
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

            val account: BlockchainAccount = mockk {
                every { balance }.returns(flowOf(accountBalance))
            }

            every { hideDustFF.enabled }.returns(Single.just(true))
            every { localSettingsPrefs.areSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = true)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(currency, account).test {
                with(expectMostRecentItem()) {
                    assertEquals(true, this)
                }
            }

            verify { hideDustFF.enabled }
            verify { localSettingsPrefs.areSmallBalancesEnabled }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
            verify { account.balance }
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

            val account: BlockchainAccount = mockk {
                every { balance }.returns(flowOf(accountBalance))
            }

            every { hideDustFF.enabled }.returns(Single.just(true))
            every { localSettingsPrefs.areSmallBalancesEnabled }.returns(true)
            every {
                watchlistService.isAssetInWatchlist(
                    currency, FreshnessStrategy.Cached(forceRefresh = true)
                )
            }.returns(
                flowOf(
                    DataResource.Data(false)
                )
            )

            subject.invoke(currency, account).test {
                with(expectMostRecentItem()) {
                    assertEquals(true, this)
                }
            }

            verify { hideDustFF.enabled }
            verify { localSettingsPrefs.areSmallBalancesEnabled }
            verify { watchlistService.isAssetInWatchlist(currency, FreshnessStrategy.Cached(forceRefresh = true)) }
            verify { account.balance }
            verify { accountBalance.totalFiat }
        }
}
