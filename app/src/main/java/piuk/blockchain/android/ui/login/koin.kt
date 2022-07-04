package piuk.blockchain.android.ui.login

import com.blockchain.koin.accountUnificationFeatureFlag
import com.blockchain.koin.appMaintenanceFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.login.auth.LoginAuthInteractor
import piuk.blockchain.android.ui.login.auth.LoginAuthModel
import piuk.blockchain.android.ui.login.auth.LoginAuthState

val loginUiModule = module {

    scope(payloadScopeQualifier) {
        factory {
            LoginModel(
                initialState = LoginState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                interactor = get(),
                getAppMaintenanceConfigUseCase = get(),
                appMaintenanceFF = get(appMaintenanceFeatureFlag),
                analytics = get()
            )
        }

        factory {
            LoginInteractor(
                authDataManager = get(),
                payloadDataManager = get(),
                authPrefs = get(),
                appUtil = get()
            )
        }

        factory {
            LoginAuthModel(
                initialState = LoginAuthState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                interactor = get()
            )
        }

        factory {
            LoginAuthInteractor(
                authDataManager = get(),
                payloadDataManager = get(),
                authPrefs = get(),
                accountUnificationFF = get(accountUnificationFeatureFlag),
                walletStatusPrefs = get()
            )
        }
    }
}
