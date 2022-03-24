package piuk.blockchain.android.ui.home

import com.blockchain.koin.entitySwitchSilverEligibilityFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.uiTourFeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
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
                walletConnectServiceAPI = get(),
                uiTourFeatureFlag = get(uiTourFeatureFlag),
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
                paymentsDataManager = get(),
                simpleBuySync = get(),
                userIdentity = get(),
                upsellManager = get(),
                database = get(),
                credentialsWiper = get(),
                qrScanResultProcessor = get(),
                secureChannelManager = get(),
                cancelOrderUseCase = get(),
                entitySwitchSilverEligibilityFeatureFlag = get(entitySwitchSilverEligibilityFeatureFlag),
                onboardingPrefs = get()
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
