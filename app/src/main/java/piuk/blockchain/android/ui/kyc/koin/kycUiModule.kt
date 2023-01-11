@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.kyc.koin

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressNextStepDecision
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.countryselection.KycCountrySelectionPresenter
import piuk.blockchain.android.ui.kyc.invalidcountry.KycInvalidCountryPresenter
import piuk.blockchain.android.ui.kyc.limits.KycLimitsInteractor
import piuk.blockchain.android.ui.kyc.limits.KycLimitsModel
import piuk.blockchain.android.ui.kyc.mobile.entry.KycMobileEntryPresenter
import piuk.blockchain.android.ui.kyc.mobile.validation.KycMobileValidationPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostPresenter
import piuk.blockchain.android.ui.kyc.profile.KycProfileModel
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.TiersReentryDecision
import piuk.blockchain.android.ui.kyc.status.KycStatusPresenter
import piuk.blockchain.android.ui.kyc.tiersplash.KycTierSplashPresenter
import piuk.blockchain.android.ui.kyc.veriffsplash.VeriffSplashModel

val kycUiModule = module {

    scope(payloadScopeQualifier) {

        factory {
            TiersReentryDecision(
                custodialWalletManager = get(),
                dataRemediationService = get(),
                kycService = get(),
            )
        }.bind(ReentryDecision::class)

        factory {
            ReentryDecisionKycNavigator(
                userService = get(),
                reentryDecision = get(),
                analytics = get()
            )
        }.bind(KycNavigator::class)

        factory {
            KycTierSplashPresenter(
                kycService = get(),
                analytics = get()
            )
        }

        factory {
            KycCountrySelectionPresenter(
                eligibilityService = get()
            )
        }

        viewModel {
            KycProfileModel(
                analytics = get(),
                nabuDataManager = get(),
                userService = get(),
                getUserStore = get(),
            )
        }

        factory {
            KycHomeAddressPresenter(
                nabuDataManager = get(),
                eligibilityService = get(),
                userService = get(),
                nabuUserSync = get(),
                custodialWalletManager = get(),
                kycNextStepDecision = get(),
                analytics = get(),
                kycTiersStore = get(),
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
                dataRemediationService = get()
            )
        }

        viewModel {
            VeriffSplashModel(
                userService = get(),
                custodialWalletManager = get(),
                nabuDataManager = get(),
                kycTiersStore = get(),
                analytics = get(),
                sessionPrefs = get(),
            )
        }

        factory {
            KycStatusPresenter(
                kycStatusHelper = get(),
                notificationTokenManager = get()
            )
        }

        factory {
            KycNavHostPresenter(
                userService = get(),
                reentryDecision = get(),
                kycNavigator = get(),
                kycTiersStore = get(),
                getUserStore = get(),
                productEligibilityStore = get(),
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
                kycService = get()
            )
        }
    }
}

val kycUiNabuModule = module {

    scope(payloadScopeQualifier) {

        factory {
            KycHomeAddressNextStepDecision(
                userService = get(),
                dataRemediationService = get()
            )
        }
    }
}
