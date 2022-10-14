package piuk.blockchain.android.ui.settings.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.presentation.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetAddPaymentMethodBinding
import piuk.blockchain.android.util.StringLocalizationUtil

class AddPaymentMethodsBottomSheet : SlidingModalBottomDialog<DialogSheetAddPaymentMethodBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onAddCardSelected()
        fun onLinkBankSelected()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a AddPaymentMethodsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetAddPaymentMethodBinding =
        DialogSheetAddPaymentMethodBinding.inflate(inflater, container, false)

    private val canAddCard by lazy {
        arguments?.getBoolean(CAN_ADD_CARD, false) ?: false
    }
    private val canLinkBank by lazy {
        arguments?.getBoolean(CAN_ADD_LINK_BANK, false) ?: false
    }

    private val fiatCurrenciesService: FiatCurrenciesService by scopedInject()

    override fun initControls(binding: DialogSheetAddPaymentMethodBinding) {
        with(binding) {
            with(addCardParent) {
                primaryText = getString(R.string.add_credit_or_debit_card_1)
                startImageResource = ImageResource.Local(R.drawable.ic_payment_card, null)
                secondaryText = getString(R.string.buy_small_amounts)
                paragraphText = getString(R.string.instantly_buy_crypto_with_card)
                tags = listOf(TagViewState(getString(R.string.most_popular), TagType.Success()))
                onClick = {
                    dismiss()
                    host.onAddCardSelected()
                }
                visibleIf { canAddCard }
            }

            with(addBankLinkParent) {
                primaryText = getString(R.string.easy_bank_transfer)
                startImageResource = ImageResource.Local(R.drawable.ic_bank_icon, null)
                secondaryText = getString(
                    StringLocalizationUtil.subtitleForEasyTransfer(
                        fiatCurrenciesService.selectedTradingCurrency.networkTicker
                    )
                )
                paragraphText = getString(R.string.easy_bank_transfer_blurb)
                onClick = {
                    dismiss()
                    host.onLinkBankSelected()
                }
                visibleIf { canLinkBank }
            }
        }
    }

    companion object {
        private const val CAN_ADD_CARD = "CAN_ADD_CARD"
        private const val CAN_ADD_LINK_BANK = "CAN_ADD_LINK_BANK"

        fun newInstance(
            canAddCard: Boolean,
            canLinkBank: Boolean,
        ): AddPaymentMethodsBottomSheet {
            val bundle = Bundle()
            bundle.putBoolean(CAN_ADD_CARD, canAddCard)
            bundle.putBoolean(CAN_ADD_LINK_BANK, canLinkBank)
            return AddPaymentMethodsBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}
