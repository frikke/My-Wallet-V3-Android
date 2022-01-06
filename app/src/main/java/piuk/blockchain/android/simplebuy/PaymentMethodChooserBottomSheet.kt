package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.nabu.datamanagers.PaymentMethod
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyPaymentMethodChooserBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class PaymentMethodChooserBottomSheet : SlidingModalBottomDialog<SimpleBuyPaymentMethodChooserBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onPaymentMethodChanged(paymentMethod: PaymentMethod)
        fun showAvailableToAddPaymentMethods()
    }

    private val paymentMethods: List<PaymentMethod> by unsafeLazy {
        arguments?.getSerializable(SUPPORTED_PAYMENT_METHODS) as? List<PaymentMethod>
            ?: emptyList()
    }

    private val displayedMode: DisplayMode by unsafeLazy {
        arguments?.getSerializable(DISPLAY_MODE) as DisplayMode
    }

    private val assetResources: AssetResources by inject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SimpleBuyPaymentMethodChooserBinding =
        SimpleBuyPaymentMethodChooserBinding.inflate(inflater, container, false)

    override fun initControls(binding: SimpleBuyPaymentMethodChooserBinding) {
        binding.recycler.apply {
            adapter =
                PaymentMethodsAdapter(
                    paymentMethods
                        .map {
                            it.toPaymentMethodItem()
                        },
                    assetResources
                )
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            layoutManager = LinearLayoutManager(context)
        }
        val isShowingPaymentMethods = displayedMode == DisplayMode.PAYMENT_METHODS

        binding.addPaymentMethod.visibleIf { isShowingPaymentMethods }
        binding.title.text =
            if (isShowingPaymentMethods) getString(R.string.pay_with_my_dotted) else getString(R.string.payment_methods)
        binding.addPaymentMethod.setOnClickListener {
            (host as? Host)?.showAvailableToAddPaymentMethods()
            dismiss()
        }

        analytics.logEvent(paymentMethodsShown(paymentMethods.map { it.toAnalyticsString() }.joinToString { "," }))
    }

    private fun PaymentMethod.toPaymentMethodItem(): PaymentMethodItem {
        return PaymentMethodItem(this, clickAction())
    }

    private fun PaymentMethod.canBeDisplayedAsPayingMethod(): Boolean =
        canBeUsedForPaying() || this is PaymentMethod.UndefinedBankAccount

    private fun PaymentMethod.clickAction(): () -> Unit =
        {
            (host as? Host)?.onPaymentMethodChanged(this)
            dismiss()
        }

    companion object {
        private const val SUPPORTED_PAYMENT_METHODS = "supported_payment_methods_key"
        private const val DISPLAY_MODE = "DISPLAY_MODE"

        fun newInstance(
            paymentMethods: List<PaymentMethod>,
            mode: DisplayMode
        ): PaymentMethodChooserBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SUPPORTED_PAYMENT_METHODS, paymentMethods as Serializable)
            bundle.putSerializable(DISPLAY_MODE, mode)
            return PaymentMethodChooserBottomSheet().apply {
                arguments = bundle
            }
        }
    }

    enum class DisplayMode {
        PAYMENT_METHODS, PAYMENT_METHOD_TYPES
    }
}

data class PaymentMethodItem(val paymentMethod: PaymentMethod, val clickAction: () -> Unit)

private class PaymentMethodsAdapter(
    adapterItems: List<PaymentMethodItem>,
    assetResources: AssetResources
) :
    DelegationAdapter<PaymentMethodItem>(AdapterDelegatesManager(), adapterItems) {
    init {
        val cardPaymentDelegate = CardPaymentDelegate()
        val bankPaymentDelegate = BankPaymentDelegate()
        val addFundsPaymentDelegate = AddFundsDelegate()
        val addCardPaymentDelegate = AddCardDelegate()
        val linkBankPaymentDelegate = LinkBankDelegate()
        val fundsPaymentDelegate = FundsPaymentDelegate(assetResources)

        delegatesManager.apply {
            addAdapterDelegate(cardPaymentDelegate)
            addAdapterDelegate(fundsPaymentDelegate)
            addAdapterDelegate(addCardPaymentDelegate)
            addAdapterDelegate(linkBankPaymentDelegate)
            addAdapterDelegate(bankPaymentDelegate)
            addAdapterDelegate(addFundsPaymentDelegate)
        }
    }
}
