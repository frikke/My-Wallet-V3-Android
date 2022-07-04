package piuk.blockchain.android.ui.dashboard.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatCurrency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.LinkBankMethodChooserSheetLayoutBinding
import piuk.blockchain.android.databinding.LinkBankMethodItemBinding
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.settings.v2.BankLinkingHost
import piuk.blockchain.android.util.StringLocalizationUtil

class LinkBankMethodChooserBottomSheet : SlidingModalBottomDialog<LinkBankMethodChooserSheetLayoutBinding>() {
    private val paymentMethods: LinkablePaymentMethodsForAction
        get() = arguments?.getSerializable(LINKABLE_METHODS) as LinkablePaymentMethodsForAction

    private val isForPayment: Boolean
        get() = arguments?.getBoolean(FOR_PAYMENT, false) ?: false

    private val currencyPrefs: CurrencyPrefs by inject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): LinkBankMethodChooserSheetLayoutBinding =
        LinkBankMethodChooserSheetLayoutBinding.inflate(inflater, container, false)

    override val host: BankLinkingHost
        get() = parentFragment as? BankLinkingHost
            ?: throw IllegalStateException("Host is not a BankLinkingHost")

    private fun launchOrigin(): LaunchOrigin {
        return when (paymentMethods) {
            is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForSettings -> LaunchOrigin.SETTINGS
            is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit -> LaunchOrigin.DEPOSIT
            is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw -> LaunchOrigin.WITHDRAW
        }
    }

    override fun initControls(binding: LinkBankMethodChooserSheetLayoutBinding) {
        with(binding) {
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = LinkBankMethodChooserAdapter(
                paymentMethods = paymentMethods.linkablePaymentMethods.linkMethods,
                tradingCurrency = currencyPrefs.tradingCurrency,
                onClick = {
                    analytics.logEvent(BankAuthAnalytics.LinkBankSelected(launchOrigin()))
                    when (it) {
                        PaymentMethodType.BANK_TRANSFER -> kotlin.run {
                            host.onLinkBankSelected(paymentMethods)
                            dismiss()
                        }
                        PaymentMethodType.BANK_ACCOUNT -> kotlin.run {
                            host.onBankWireTransferSelected(paymentMethods.linkablePaymentMethods.currency)
                            dismiss()
                        }
                        else -> throw IllegalStateException("Not supported linking method")
                    }
                }
            )

            paymentMethodsTitle.text = getString(
                if (isForPayment) {
                    R.string.add_a_deposit_method
                } else {
                    R.string.add_a_bank_account
                }
            )
        }
    }

    companion object {
        private const val LINKABLE_METHODS = "LINKABLE_METHODS"
        private const val FOR_PAYMENT = "FOR_PAYMENT"

        fun newInstance(
            linkablePaymentMethodsForAction: LinkablePaymentMethodsForAction,
            isForPayment: Boolean = false
        ): LinkBankMethodChooserBottomSheet =
            LinkBankMethodChooserBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(LINKABLE_METHODS, linkablePaymentMethodsForAction)
                    putBoolean(FOR_PAYMENT, isForPayment)
                }
            }
    }
}

class LinkBankMethodChooserAdapter(
    private val paymentMethods: List<PaymentMethodType>,
    private val tradingCurrency: FiatCurrency,
    private val onClick: (PaymentMethodType) -> Unit,
) : RecyclerView.Adapter<LinkBankMethodChooserAdapter.LinkBankMethodViewHolder>() {

    class LinkBankMethodViewHolder(
        private val binding: LinkBankMethodItemBinding,
        private val tradingCurrency: FiatCurrency
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethod: PaymentMethodType, onClick: (PaymentMethodType) -> Unit) {
            val item = paymentMethod.toLinkBankMethodItemUI(tradingCurrency)

            with(binding) {
                paymentMethodTitle.setText(item.title)
                paymentMethodSubtitle.setText(item.subtitle)
                paymentMethodBlurb.setText(item.blurb)
                paymentMethodIcon.setImageResource(item.icon)
                paymentMethodRoot.setOnClickListener {
                    onClick(paymentMethod)
                }
                badge.visibleIf { paymentMethod == PaymentMethodType.BANK_TRANSFER }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkBankMethodViewHolder {
        val binding = LinkBankMethodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkBankMethodViewHolder(
            binding = binding,
            tradingCurrency = tradingCurrency
        )
    }

    override fun onBindViewHolder(holder: LinkBankMethodViewHolder, position: Int) {
        holder.bind(paymentMethods[position], onClick)
    }

    override fun getItemCount(): Int = paymentMethods.size
}

private fun PaymentMethodType.toLinkBankMethodItemUI(
    fiatCurrency: FiatCurrency,
): LinkBankMethodItem =
    when (this) {
        PaymentMethodType.BANK_ACCOUNT -> LinkBankMethodItem(
            title = StringLocalizationUtil.getBankDepositTitle(fiatCurrency.networkTicker),
            subtitle = StringLocalizationUtil.subtitleForBankAccount(fiatCurrency),
            blurb = StringLocalizationUtil.blurbForBankAccount(fiatCurrency),
            icon = R.drawable.ic_funds_deposit
        )
        PaymentMethodType.BANK_TRANSFER -> LinkBankMethodItem(
            title = R.string.easy_bank_transfer,
            subtitle = StringLocalizationUtil.subtitleForEasyTransfer(fiatCurrency),
            blurb = R.string.easy_bank_transfer_blurb,
            icon = R.drawable.ic_bank_transfer
        )
        else -> throw IllegalStateException("Not supported linking method")
    }

data class LinkBankMethodItem(
    @StringRes val title: Int,
    @StringRes val blurb: Int,
    @DrawableRes val icon: Int,
    @StringRes val subtitle: Int
)
