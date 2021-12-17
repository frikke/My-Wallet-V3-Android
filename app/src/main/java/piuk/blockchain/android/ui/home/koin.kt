package piuk.blockchain.android.ui.home

import com.blockchain.koin.fabSheetOrderingFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.redesignPart2FeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.home.flags.FabSheetBuySellOrderingFeatureFlag
import piuk.blockchain.android.ui.home.flags.RedesignPart2FeatureFlag
import piuk.blockchain.android.ui.home.models.ActionsSheetInteractor
import piuk.blockchain.android.ui.home.models.ActionsSheetModel
import piuk.blockchain.android.ui.home.models.ActionsSheetState
import piuk.blockchain.android.ui.home.models.MainInteractor
import piuk.blockchain.android.ui.home.models.MainModel
import piuk.blockchain.android.ui.home.models.MainState

val mainModule = module {

    scope(payloadScopeQualifier) {
        factory {
            MainModel(
                initialState = MainState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            MainInteractor(
                deepLinkProcessor = get(),
                exchangeLinking = get(),
                exchangePrefs = get(),
                assetCatalogue = get(),
                xlmDataManager = get(),
                sunriverCampaignRegistration = get(),
                kycStatusHelper = get(),
                bankLinkingPrefs = get(),
                custodialWalletManager = get(),
                simpleBuySync = get(),
                userIdentity = get(),
                upsellManager = get(),
                database = get(),
                credentialsWiper = get(),
                qrScanResultProcessor = get(),
                secureChannelManager = get(),
                cancelOrderUseCase = get()
            )
        }

        factory {
            ActionsSheetModel(
                initialState = ActionsSheetState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            ActionsSheetInteractor(
                userIdentity = get(),
                fabSheetBuySellOrderingFeatureFlag = get()
            )
        }

        factory {
            FabSheetBuySellOrderingFeatureFlag(
                localApi = get(),
                remoteConfig = get(fabSheetOrderingFeatureFlag)
            )
        }

        factory {
            RedesignPart2FeatureFlag(
                localApi = get(),
                remoteConfig = get(redesignPart2FeatureFlag)
            )
        }
    }
}
