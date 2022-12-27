package piuk.blockchain.android.domain.usecases

import com.blockchain.domain.buy.CancelOrderService
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.toPreferencesValue
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Completable

class CancelOrderUseCase(
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val custodialWalletManager: CustodialWalletManager
) : UseCase<String, Completable>(), CancelOrderService {

    override fun execute(parameter: String): Completable {
        bankLinkingPrefs.setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        return custodialWalletManager.getBuyOrder(parameter).flatMapCompletable {
            if (it.state <= OrderState.PENDING_CONFIRMATION) {
                custodialWalletManager.deleteBuyOrder(it.id)
            } else {
                Completable.complete()
            }
        }.onErrorComplete()
    }

    override fun cancelOrder(orderId: String): Completable {
        return execute(orderId)
    }
}
