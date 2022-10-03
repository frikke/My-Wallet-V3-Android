package piuk.blockchain.android.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.payments.toCardType
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.databinding.RemoveCardBottomSheetBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RemoveCardBottomSheet : SlidingModalBottomDialog<RemoveCardBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCardRemoved(cardId: String)
    }

    private val cardService: CardService by scopedInject()

    private val card: PaymentMethod.Card by unsafeLazy {
        arguments?.getSerializable(CARD_KEY) as? PaymentMethod.Card
            ?: throw IllegalStateException("No card provided")
    }

    private val compositeDisposable = CompositeDisposable()

    override fun initControls(binding: RemoveCardBottomSheetBinding) {
        with(binding) {
            title.text = card.uiLabel()
            endDigits.text = card.dottedEndDigits()
            icon.setImageResource(card.cardType.toCardType().icon())
            rmvCardBtn.setOnClickListener {
                compositeDisposable += cardService.deleteCard(card.id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        updateUi(true)
                    }
                    .doFinally {
                        updateUi(false)
                    }
                    .subscribeBy(onComplete = {
                        (host as? Host)?.onCardRemoved(card.cardId)
                        dismiss()
                        analytics.logEvent(SimpleBuyAnalytics.REMOVE_CARD)
                    }, onError = {})

                analytics.logEvent(SettingsAnalytics.RemoveCardClicked(LaunchOrigin.SETTINGS))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun updateUi(isLoading: Boolean) {
        with(binding) {
            progress.visibleIf { isLoading }
            icon.visibleIf { !isLoading }
            rmvCardBtn.isEnabled = !isLoading
        }
    }

    companion object {
        private const val CARD_KEY = "CARD_KEY"

        fun newInstance(card: PaymentMethod.Card) =
            RemoveCardBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(CARD_KEY, card)
                }
            }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): RemoveCardBottomSheetBinding =
        RemoveCardBottomSheetBinding.inflate(inflater, container, false)
}
