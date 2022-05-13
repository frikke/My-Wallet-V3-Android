package piuk.blockchain.android.ui.auth.newlogin.presentation

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

val secureChannelPresentationModule = module {

    scope(payloadScopeQualifier) {
        factory {
            AuthNewLoginModel(
                initialState = AuthNewLoginState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                secureChannelService = get(),
                secureChannelPrefs = get(),
                walletApi = get()
            )
        }
    }
}
