package piuk.blockchain.android.ui.dashboard

import com.blockchain.core.announcements.DismissClock
import com.blockchain.core.announcements.DismissRecorder
import com.blockchain.koin.assetOrderingFeatureFlag
import com.blockchain.koin.defaultOrder
import com.blockchain.koin.exchangeWAPromptFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.paymentUxAssetDisplayBalanceFeatureFlag
import com.blockchain.koin.sellOrder
import com.blockchain.koin.swapSourceOrder
import com.blockchain.koin.swapTargetOrder
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.domain.usecases.ShouldShowExchangeCampaignUseCase
import piuk.blockchain.android.ui.cowboys.CowboysPromoDataProvider
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.android.ui.dashboard.model.ShouldAssetShowUseCase
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting
import piuk.blockchain.android.ui.transfer.SellAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapSourceAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapTargetAccountsSorting

val dashboardModule = module {

    scope(payloadScopeQualifier) {

        factory {
            ShouldAssetShowUseCase(
                assetDisplayBalanceFF = get(paymentUxAssetDisplayBalanceFeatureFlag),
                localSettingsPrefs = get(),
                watchlistService = get()
            )
        }

        factory {
            StateAwareActionsComparator()
        }.bind(Comparator::class)

        factory {
            BalanceAnalyticsReporter(
                analytics = get()
            )
        }

        factory(defaultOrder) {
            DefaultAccountsSorting(currencyPrefs = get())
        }.bind(AccountsSorting::class)

        factory(swapSourceOrder) {
            SwapSourceAccountsSorting(
                assetListOrderingFF = get(assetOrderingFeatureFlag),
                dashboardAccountsSorter = get(defaultOrder),
                sellAccountsSorting = get(sellOrder),
            )
        }.bind(AccountsSorting::class)

        factory(swapTargetOrder) {
            SwapTargetAccountsSorting(
                currencyPrefs = get(),
                exchangeRatesDataManager = get(),
                watchlistDataManager = get()
            )
        }.bind(AccountsSorting::class)

        factory(sellOrder) {
            SellAccountsSorting(
                coincore = get()
            )
        }.bind(AccountsSorting::class)

        scoped { WalletModeBalanceCache(coincore = get()) }

        scoped {
            CowboysPromoDataProvider(
                config = get(),
                json = get()
            )
        }

        factory {
            ShouldShowExchangeCampaignUseCase(
                exchangeWAPromptFF = get(exchangeWAPromptFeatureFlag),
                exchangeCampaignPrefs = get(),
                mercuryExperimentsService = get()
            )
        }
    }

    single {
        DismissRecorder(
            prefs = get(),
            clock = get()
        )
    }

    single {
        object : DismissClock {
            override fun now(): Long = System.currentTimeMillis()
        }
    }.bind(DismissClock::class)
}
