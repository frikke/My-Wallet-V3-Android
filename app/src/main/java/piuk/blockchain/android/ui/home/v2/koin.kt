package piuk.blockchain.android.ui.home.v2

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

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
                userIdentity = get()
            )
        }
    }
}
