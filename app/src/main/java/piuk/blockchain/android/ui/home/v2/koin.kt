package piuk.blockchain.android.ui.home.v2

import com.blockchain.koin.fabSheetOrderingFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.redesignPart2FeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.home.v2.flags.FabSheetBuySellOrderingFeatureFlag
import piuk.blockchain.android.ui.home.v2.flags.RedesignPart2FeatureFlag

val mainModule = module {

    scope(payloadScopeQualifier) {
        factory {
            RedesignModel(
                initialState = RedesignState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            RedesignInteractor(
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
                secureChannelManager = get()
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
