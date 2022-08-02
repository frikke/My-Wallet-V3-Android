package piuk.blockchain.android.ui.home

import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppFeatureFlag
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.dashboard.walletmode.WalletModeSelectionViewModel
import piuk.blockchain.android.ui.home.models.ActionsSheetInteractor
import piuk.blockchain.android.ui.home.models.ActionsSheetModel
import piuk.blockchain.android.ui.home.models.ActionsSheetState
import piuk.blockchain.android.ui.home.models.MainInteractor
import piuk.blockchain.android.ui.home.models.MainModel
import piuk.blockchain.android.ui.home.models.MainState
import piuk.blockchain.android.walletmode.WalletModeRepository

val mainModule = module {

    scope(payloadScopeQualifier) {
        factory {
            MainModel(
                initialState = MainState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                walletConnectServiceAPI = get(),
                environmentConfig = get(),
                remoteLogger = get(),
                walletModeService = get(),
                deeplinkingV2FF = get(deeplinkingFeatureFlag)
            )
        }

        factory {
            MainInteractor(
                deepLinkProcessor = get(),
                deeplinkRedirector = get(),
                deepLinkPersistence = get(),
                exchangeLinking = get(),
                assetCatalogue = get(),
                bankLinkingPrefs = get(),
                bankService = get(),
                simpleBuySync = get(),
                userIdentity = get(),
                upsellManager = get(),
                database = get(),
                credentialsWiper = get(),
                qrScanResultProcessor = get(),
                secureChannelService = get(),
                cancelOrderUseCase = get(),
                onboardingPrefs = get(),
                referralPrefs = get(),
                referralRepository = get()
            )
        }

        factory {
            ActionsSheetModel(
                initialState = ActionsSheetState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            ActionsSheetInteractor(
                userIdentity = get()
            )
        }
        viewModel {
            WalletModeSelectionViewModel(
                walletModeService = get(),
                cache = get(),
                payloadManager = get(),
                walletStatusPrefs = get()
            )
        }
    }

    single {
        WalletModeRepository(
            sharedPreferences = get(),
            featureFlag = get(superAppFeatureFlag)
        )
    }.bind(WalletModeService::class)
}
