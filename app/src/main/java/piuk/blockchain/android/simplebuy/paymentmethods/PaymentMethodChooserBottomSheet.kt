package piuk.blockchain.android.simplebuy.paymentmethods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.koin.scopedInject
import info.blockchain.balance.FiatCurrency
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyPaymentMethodChooserBinding
import piuk.blockchain.android.simplebuy.BankTransferViewed
import piuk.blockchain.android.simplebuy.BuyMethodOptionsViewed
import piuk.blockchain.android.simplebuy.paymentMethodsShown
import piuk.blockchain.android.simplebuy.toAnalyticsString
import piuk.blockchain.android.simplebuy.toPaymentTypeAnalyticsString
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.resources.AssetResources
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

    private val canAddNewPayment: Boolean by unsafeLazy {
        arguments?.getBoolean(CAN_ADD_NEW_PAYMENT) ?: false
    }

    private val displayedMode: DisplayMode by unsafeLazy {
        arguments?.getSerializable(DISPLAY_MODE) as DisplayMode
    }

    private val canUseCreditCards: Boolean by unsafeLazy {
        arguments?.getBoolean(CAN_USE_CREDIT_CARDS) ?: true
    }

    private val assetResources: AssetResources by inject()
    private val fiatCurrenciesService: FiatCurrenciesService by scopedInject()

    private val fiatCurrency: FiatCurrency
        get() = fiatCurrenciesService.selectedTradingCurrency

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
                    assetResources,
                    canUseCreditCards
                )
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            layoutManager = LinearLayoutManager(context)
        }
        val isShowingPaymentMethods = displayedMode == DisplayMode.PAYMENT_METHODS

        binding.addPaymentMethod.visibleIf { isShowingPaymentMethods && canAddNewPayment }
        binding.title.text =
            if (isShowingPaymentMethods) getString(R.string.pay_with_my_dotted) else getString(R.string.payment_methods)
        binding.addPaymentMethod.setOnClickListener {
            (host as? Host)?.showAvailableToAddPaymentMethods()
            dismiss()
        }

        analytics.logEvent(paymentMethodsShown(paymentMethods.map { it.toAnalyticsString() }.joinToString { "," }))
        if (isShowingPaymentMethods) {
            analytics.logEvent(BuyMethodOptionsViewed(paymentMethods.map { it.toPaymentTypeAnalyticsString() }))
        }

        if (paymentMethods.any { it is PaymentMethod.UndefinedBankTransfer }) {
            analytics.logEvent(BankTransferViewed(fiatCurrency = fiatCurrency))
        }
    }

    private fun PaymentMethod.toPaymentMethodItem(): PaymentMethodItem {
        return PaymentMethodItem(this, clickAction())
    }

    private fun PaymentMethod.clickAction(): () -> Unit =
        {
            (host as? Host)?.onPaymentMethodChanged(this)
            dismiss()
        }

    companion object {
        private const val SUPPORTED_PAYMENT_METHODS = "supported_payment_methods_key"
        private const val CAN_ADD_NEW_PAYMENT = "CAN_ADD_NEW_PAYMENT"
        private const val DISPLAY_MODE = "DISPLAY_MODE"
        private const val CAN_USE_CREDIT_CARDS = "CAN_USE_CREDIT_CARDS"

        fun newInstance(
            paymentMethods: List<PaymentMethod>,
            mode: DisplayMode,
            canAddNewPayment: Boolean,
            canUseCreditCards: Boolean = true
        ): PaymentMethodChooserBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SUPPORTED_PAYMENT_METHODS, paymentMethods as Serializable)
            bundle.putSerializable(DISPLAY_MODE, mode)
            bundle.putBoolean(CAN_ADD_NEW_PAYMENT, canAddNewPayment)
            bundle.putBoolean(CAN_USE_CREDIT_CARDS, canUseCreditCards)
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
    assetResources: AssetResources,
    canUseCreditCards: Boolean
) :
    DelegationAdapter<PaymentMethodItem>(AdapterDelegatesManager(), adapterItems) {
    init {
        val cardPaymentDelegate = CardPaymentDelegate()
        val bankPaymentDelegate = BankPaymentDelegate()
        val depositTooltipDelegate = DepositTooltipDelegate()
        val addCardPaymentDelegate = AddCardDelegate(canUseCreditCards)
        val linkBankPaymentDelegate = LinkBankDelegate()
        val fundsPaymentDelegate = FundsPaymentDelegate(assetResources)
        val googlePayDelegate = GooglePayDelegate()

        delegatesManager.apply {
            addAdapterDelegate(cardPaymentDelegate)
            addAdapterDelegate(fundsPaymentDelegate)
            addAdapterDelegate(addCardPaymentDelegate)
            addAdapterDelegate(linkBankPaymentDelegate)
            addAdapterDelegate(bankPaymentDelegate)
            addAdapterDelegate(depositTooltipDelegate)
            addAdapterDelegate(googlePayDelegate)
        }
    }
}
