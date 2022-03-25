@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.kyc.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.EthEligibility
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.kyc.additional_info.KycAdditionalInfoModel
import piuk.blockchain.android.ui.kyc.additional_info.KycAdditionalInfoNextStepDecision
import piuk.blockchain.android.ui.kyc.additional_info.StateMachine
import piuk.blockchain.android.ui.kyc.address.CurrentTierAdapter
import piuk.blockchain.android.ui.kyc.address.EligibilityForFreeEthAdapter
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.address.KycNextStepDecision
import piuk.blockchain.android.ui.kyc.address.KycNextStepDecisionAdapter
import piuk.blockchain.android.ui.kyc.countryselection.KycCountrySelectionPresenter
import piuk.blockchain.android.ui.kyc.invalidcountry.KycInvalidCountryPresenter
import piuk.blockchain.android.ui.kyc.limits.KycLimitsInteractor
import piuk.blockchain.android.ui.kyc.limits.KycLimitsModel
import piuk.blockchain.android.ui.kyc.mobile.entry.KycMobileEntryPresenter
import piuk.blockchain.android.ui.kyc.mobile.validation.KycMobileValidationPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycStarter
import piuk.blockchain.android.ui.kyc.navhost.StartKyc
import piuk.blockchain.android.ui.kyc.profile.KycProfilePresenter
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.TiersReentryDecision
import piuk.blockchain.android.ui.kyc.status.KycStatusPresenter
import piuk.blockchain.android.ui.kyc.tiersplash.KycTierSplashPresenter
import piuk.blockchain.android.ui.kyc.veriffsplash.VeriffSplashPresenter

val kycUiModule = module {

    factory { KycStarter() }.bind(StartKyc::class)

    scope(payloadScopeQualifier) {

        factory {
            TiersReentryDecision(
                kycDataManager = get()
            )
        }.bind(ReentryDecision::class)

        factory {
            ReentryDecisionKycNavigator(
                token = get(),
                dataManager = get(),
                reentryDecision = get(),
                analytics = get()
            )
        }.bind(KycNavigator::class)

        factory {
            KycTierSplashPresenter(
                tierUpdater = get(),
                tierService = get(),
                kycNavigator = get()
            )
        }

        factory {
            KycCountrySelectionPresenter(
                nabuDataManager = get()
            )
        }

        factory {
            KycProfilePresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                metadataRepository = get(),
                stringUtils = get()
            )
        }

        factory {
            KycHomeAddressPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                custodialWalletManager = get(),
                kycNextStepDecision = get(),
                analytics = get()
            )
        }

        factory {
            KycMobileEntryPresenter(
                phoneNumberUpdater = get(),
                nabuUserSync = get()
            )
        }

        factory {
            KycMobileValidationPresenter(
                nabuUserSync = get(),
                phoneNumberUpdater = get(),
                analytics = get()
            )
        }

        factory {
            VeriffSplashPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            KycStatusPresenter(
                nabuToken = get(),
                kycStatusHelper = get(),
                notificationTokenManager = get()
            )
        }

        factory {
            KycNavHostPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                sunriverCampaign = get(),
                reentryDecision = get(),
                tierUpdater = get(),
                kycNavigator = get(),
                analytics = get()
            )
        }

        factory {
            KycInvalidCountryPresenter(
                nabuDataManager = get(),
                metadataRepository = get()
            )
        }

        factory {
            KycLimitsModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            KycLimitsInteractor(
                limitsDataManager = get(),
                userIdentity = get()
            )
        }
    }
}

val kycUiNabuModule = module {

    scope(payloadScopeQualifier) {

        factory {
            KycNextStepDecisionAdapter(
                nabuToken = get(),
                nabuDataManager = get(),
                kycDataManager = get()
            )
        }.bind(KycNextStepDecision::class)

        factory {
            KycAdditionalInfoNextStepDecision(
                nabuToken = get(),
                nabuDataManager = get()
            )
        }

        factory {
            CurrentTierAdapter(
                nabuToken = get(),
                nabuDataManager = get()
            )
        }.bind(CurrentTier::class)

        factory {
            EligibilityForFreeEthAdapter(
                nabuToken = get(),
                nabuDataManager = get()
            )
        }.bind(EthEligibility::class)

        viewModel {
            KycAdditionalInfoModel(
                kycDataManager = get(),
                stateMachine = StateMachine(),
                custodialWalletManager = get(),
                analytics = get(),
                kycNextStepDecision = get(KycAdditionalInfoNextStepDecision::class)
            )
        }
    }
}
