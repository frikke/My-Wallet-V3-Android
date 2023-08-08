package piuk.blockchain.android.simplebuy

import com.blockchain.domain.paymentmethods.model.BankBuyAuthStep
import com.blockchain.domain.paymentmethods.model.BankBuyNavigation
import com.blockchain.nabu.datamanagers.OrderState
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.home.models.MainInteractor

class BankBuyNavigationImpl(private val interactor: MainInteractor) : BankBuyNavigation {
    override fun step(): Single<BankBuyAuthStep> {
        return interactor.getSimpleBuySyncLocalState()?.let {
            Single.just(handleOrderState(it))
        } ?: kotlin.run {
            // try to sync with server once, otherwise fail
            interactor.performSimpleBuySync()
                .toSingle {
                    interactor.getSimpleBuySyncLocalState()?.let {
                        handleOrderState(it)
                    } ?: BankBuyAuthStep.BuyWithBankError
                }.doOnError {
                    interactor.resetLocalBankAuthState()
                }.onErrorReturn {
                    BankBuyAuthStep.BuyWithBankError
                }
        }
    }

    private fun handleOrderState(state: SimpleBuyState): BankBuyAuthStep {
        return if (state.orderState == OrderState.AWAITING_FUNDS) {
            BankBuyAuthStep.BuyWithBankApproved
        } else {
            BankBuyAuthStep.BankAuthForCancelledOrder(state.fiatCurrency)
        }
    }
}
