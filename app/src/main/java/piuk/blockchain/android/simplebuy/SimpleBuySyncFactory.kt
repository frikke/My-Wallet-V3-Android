package piuk.blockchain.android.simplebuy

import androidx.annotation.VisibleForTesting
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.utils.flatMapBy
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.balance.asFiatCurrencyOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

// Ensure that the local and remote SimpleBuy state is the same.
// Resolution strategy is:
//  - check simple buy is enabled
//  - inflate the local state, if any
//  - fetch the remote state, if any
//      - if the remote state is the same as the local state, then do nothing
//      - if the remote state exists and the local state is in an earlier stage, use the remote state
//      - if the remote state and the local state refer to the same order (id) and the remote state
//        is completed/error/cancel, then wipe the local state
//

class SimpleBuySyncFactory(
    private val custodialWallet: CustodialWalletManager,
    private val bankService: BankService,
    private val cardService: CardService,
    private val serializer: SimpleBuyPrefsSerializer
) {

    fun performSync(): Completable = syncStates()
        .doOnSuccess { v ->
            Timber.d("SB Sync: Success")
            serializer.update(v)
        }
        .doOnComplete {
            Timber.d("SB Sync: Complete")
            serializer.clear()
        }
        .ignoreElement()
        .observeOn(Schedulers.computation())
        .doOnError {
            Timber.d("SB Sync: FAILED because $it")
        }

    fun currentState(): SimpleBuyState? =
        serializer.fetch().apply {
            Timber.d("SB Sync: state == $this")
        }

    fun clear() {
        serializer.clear()
    }

    fun cancelAnyPendingConfirmationBuy(): Completable {
        val currentOrder = currentState() ?: return Completable.complete()
        val pendingOrderId = currentOrder.takeIf { it.orderState == OrderState.PENDING_CONFIRMATION }?.id
            ?: return Completable.complete()

        return custodialWallet.deleteBuyOrder(pendingOrderId)
            .doOnComplete {
                clear()
            }
            .doOnError {
                Timber.e("Failed to cancel buy order $pendingOrderId")
            }
    }

    private fun syncStates(): Maybe<SimpleBuyState> {
        return maybeInflateLocalState()
            .flatMap { updateWithRemote(it) }
            .flatMapBy(
                onSuccess = { checkForRemoteOverride(it) },
                onError = {
                    Maybe.error(it)
                },
                onComplete = { Maybe.defer { getRemotePendingBuy() } }
            )
    }

    private fun getRemotePendingBuy(): Maybe<SimpleBuyState> {
        return custodialWallet.getAllOutstandingBuyOrders()
            .flatMapMaybe { list ->
                list.sortedByDescending { it.expires }
                    .firstOrNull {
                        it.state == OrderState.AWAITING_FUNDS ||
                            it.state == OrderState.PENDING_EXECUTION ||
                            it.state == OrderState.PENDING_CONFIRMATION
                    }?.toSimpleBuyStateMaybe() ?: Maybe.empty()
            }
    }

    private fun checkForRemoteOverride(localState: SimpleBuyState): Maybe<SimpleBuyState> {
        return if (localState.orderState < OrderState.PENDING_CONFIRMATION) {
            custodialWallet.getAllOutstandingBuyOrders()
                .flatMapMaybe { list ->
                    list.sortedByDescending { it.expires }
                        .firstOrNull {
                            it.state == OrderState.AWAITING_FUNDS ||
                                it.state == OrderState.PENDING_EXECUTION ||
                                it.state == OrderState.PENDING_CONFIRMATION
                        }?.toSimpleBuyStateMaybe() ?: Maybe.just(localState)
                }
        } else {
            Maybe.just(localState)
        }
    }

    private fun BuySellOrder.isDefinedCardPayment() =
        paymentMethodType == PaymentMethodType.PAYMENT_CARD &&
            paymentMethodId != PaymentMethod.UNDEFINED_CARD_PAYMENT_ID

    private fun BuySellOrder.isDefinedBankTransferPayment() =
        paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
            paymentMethodId != PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID

    private fun BuySellOrder.toSimpleBuyStateMaybe(): Maybe<SimpleBuyState> = when {
        isDefinedCardPayment() -> {
            cardService.getCardDetailsLegacy(paymentMethodId).flatMapMaybe {
                Maybe.just(
                    this.toSimpleBuyState().copy(
                        selectedPaymentMethod = SelectedPaymentMethod(
                            it.cardId,
                            it.partner,
                            it.detailedLabel(),
                            PaymentMethodType.PAYMENT_CARD,
                            true
                        )
                    )
                )
            }
        }
        isDefinedBankTransferPayment() -> {
            bankService.getLinkedBankLegacy(paymentMethodId).flatMapMaybe {
                Maybe.just(
                    toSimpleBuyState().copy(
                        selectedPaymentMethod = SelectedPaymentMethod(
                            it.id,
                            null,
                            it.accountName,
                            PaymentMethodType.BANK_TRANSFER,
                            true
                        )
                    )
                )
            }
        }
        else -> {
            Maybe.just(toSimpleBuyState())
        }
    }

    private fun updateWithRemote(localState: SimpleBuyState): Maybe<SimpleBuyState> =
        getRemoteForLocal(localState.id)
            .defaultIfEmpty(localState)
            .map { remoteState ->
                Timber.d("SB Sync: local.state == ${localState.orderState}, remote.state == ${remoteState.orderState}")
                if (localState.orderState < remoteState.orderState) {
                    Timber.d("SB Sync: Take remote")
                    remoteState
                } else {
                    Timber.d("SB Sync: Take local")
                    localState
                }
            }
            .flatMapMaybe { state ->
                when (state.orderState) {
                    OrderState.UNINITIALISED,
                    OrderState.INITIALISED,
                    OrderState.PENDING_EXECUTION,
                    OrderState.PENDING_CONFIRMATION -> Maybe.just(state)
                    OrderState.AWAITING_FUNDS,
                    OrderState.FINISHED,
                    OrderState.CANCELED,
                    OrderState.FAILED,
                    OrderState.UNKNOWN -> Maybe.empty()
                }
            }

    private fun getRemoteForLocal(id: String?): Maybe<SimpleBuyState> =
        id?.let {
            custodialWallet.getBuyOrder(it)
                .map { order -> order.toSimpleBuyState() }
                .toMaybe()
                .onErrorResumeNext { Maybe.empty() }
        } ?: Maybe.empty()

    private fun maybeInflateLocalState(): Maybe<SimpleBuyState> =
        Maybe.fromCallable { serializer.fetch() }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun BuySellOrder.toSimpleBuyState(): SimpleBuyState =
    SimpleBuyState(
        id = id,
        amount = source as FiatValue,
        fiatCurrency = source.currency.asFiatCurrencyOrThrow(),
        selectedCryptoAsset = target.currency.asAssetInfoOrThrow(),
        orderState = state,
        orderValue = orderValue as? CryptoValue,
        selectedPaymentMethod = SelectedPaymentMethod(
            id = paymentMethodId,
            paymentMethodType = paymentMethodType,
            isEligible = true
        ),
        currentScreen = FlowScreen.ENTER_AMOUNT
    )
