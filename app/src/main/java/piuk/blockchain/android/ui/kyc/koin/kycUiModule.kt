@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.kyc.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.EthEligibility
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.kyc.address.CurrentTierAdapter
import piuk.blockchain.android.ui.kyc.address.EligibilityForFreeEthAdapter
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressNextStepDecision
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.countryselection.KycCountrySelectionPresenter
import piuk.blockchain.android.ui.kyc.invalidcountry.KycInvalidCountryPresenter
import piuk.blockchain.android.ui.kyc.limits.KycLimitsInteractor
import piuk.blockchain.android.ui.kyc.limits.KycLimitsModel
import piuk.blockchain.android.ui.kyc.mobile.entry.KycMobileEntryPresenter
import piuk.blockchain.android.ui.kyc.mobile.validation.KycMobileValidationPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostPresenter
import piuk.blockchain.android.ui.kyc.profile.KycProfilePresenter
import piuk.blockchain.android.ui.kyc.questionnaire.KycQuestionnaireModel
import piuk.blockchain.android.ui.kyc.questionnaire.KycQuestionnaireStateMachine
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.TiersReentryDecision
import piuk.blockchain.android.ui.kyc.status.KycStatusPresenter
import piuk.blockchain.android.ui.kyc.tiersplash.KycTierSplashPresenter
import piuk.blockchain.android.ui.kyc.veriffsplash.VeriffSplashPresenter

val kycUiModule = module {

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
                tierService = get(),
                analytics = get()
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
                stringUtils = get(),
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
                kycDataManager = get(),
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
                reentryDecision = get(),
                tierUpdater = get(),
                kycNavigator = get(),
                analytics = get()
            )
        }

        factory {
            KycInvalidCountryPresenter(
                nabuDataManager = get()
            )
        }

        factory {
            KycLimitsModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get()
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
            KycHomeAddressNextStepDecision(
                nabuToken = get(),
                nabuDataManager = get(),
                kycDataManager = get()
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
            KycQuestionnaireModel(
                kycDataManager = get(),
                stateMachine = KycQuestionnaireStateMachine(),
                analytics = get()
            )
        }
    }
}
