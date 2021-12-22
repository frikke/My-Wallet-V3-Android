package piuk.blockchain.android.ui.settings.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetAddPaymentMethodBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.visibleIf

class AddPaymentMethodsBottomSheet : SlidingModalBottomDialog<DialogSheetAddPaymentMethodBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onAddCardSelected()
        fun onAddBankTransferSelected()
        fun onAddBankAccountSelected()
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
    private val canAddBankTransfer by lazy {
        arguments?.getBoolean(CAN_ADD_BANK_TRANSFER, false) ?: false
    }
    private val canAddBankAccount by lazy {
        arguments?.getBoolean(CAN_ADD_BANK_ACCOUNT, false) ?: false
    }

    override fun initControls(binding: DialogSheetAddPaymentMethodBinding) {
        with(binding) {
            with(addCardParent) {
                primaryText = getString(R.string.add_credit_or_debit_card_1)
                startImageResource = ImageResource.Local(R.drawable.ic_payment_card, null)
                secondaryText = getString(R.string.instantly_available)
                paragraphText = getString(R.string.instantly_buy_crypto_with_card)
                tags = listOf(TagViewState(getString(R.string.most_popular), TagType.Success()))
                onClick = {
                    dismiss()
                    host.onAddCardSelected()
                }
                visibleIf { canAddCard }
            }

            with(addBankLinkParent) {
                primaryText = getString(R.string.link_a_bank)
                startImageResource = ImageResource.Local(R.drawable.ic_bank_transfer, null)
                secondaryText = getString(R.string.instantly_available)
                paragraphText = getString(R.string.instantly_buy_crypto_with_link_a_bank)
                onClick = {
                    dismiss()
                    host.onAddBankTransferSelected()
                }
                visibleIf { canAddBankTransfer }
            }

            with(addBankAccountParent) {
                primaryText = getString(R.string.bank_transfer)
                startImageResource = ImageResource.Local(R.drawable.ic_funds_deposit, null)
                secondaryText = getString(R.string.payment_wire_transfer_subtitle)
                paragraphText = getString(R.string.settings_bank_transfer_blurb)
                onClick = {
                    dismiss()
                    host.onAddBankAccountSelected()
                }
                visibleIf { canAddBankAccount }
            }
        }
    }

    companion object {
        private const val CAN_ADD_CARD = "CAN_ADD_CARD"
        private const val CAN_ADD_BANK_TRANSFER = "CAN_ADD_BANK_TRANSFER"
        private const val CAN_ADD_BANK_ACCOUNT = "CAN_ADD_BANK_ACCOUNT"

        fun newInstance(
            canAddCard: Boolean,
            canAddBankTransfer: Boolean,
            canAddBankAccount: Boolean,
        ): AddPaymentMethodsBottomSheet {
            val bundle = Bundle()
            bundle.putBoolean(CAN_ADD_CARD, canAddCard)
            bundle.putBoolean(CAN_ADD_BANK_TRANSFER, canAddBankTransfer)
            bundle.putBoolean(CAN_ADD_BANK_ACCOUNT, canAddBankAccount)
            return AddPaymentMethodsBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}
