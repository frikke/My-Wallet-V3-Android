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
            RedesignInteractor()
        }
    }
}
