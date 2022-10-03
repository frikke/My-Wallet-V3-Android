package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FundsPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.resources.AssetResources

class FundsPaymentDelegate(private val assetResources: AssetResources) : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Funds

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsPaymentViewHolder(
            FundsPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            assetResources
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FundsPaymentViewHolder).bind(items[position])
    }

    private class FundsPaymentViewHolder(
        private val binding: FundsPaymentMethodLayoutBinding,
        private val assetResources: AssetResources
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Funds)?.let {
                    fundsPayment.apply {
                        titleStart = buildAnnotatedString { append(it.fiatCurrency.name) }
                        titleEnd = buildAnnotatedString { append(it.balance.toStringWithSymbol()) }
                        bodyStart = buildAnnotatedString { append(it.fiatCurrency.displayTicker) }
                        startImageResource = if (it.fiatCurrency.logo.isNotEmpty()) {
                            ImageResource.Remote(it.fiatCurrency.logo)
                        } else {
                            ImageResource.Local(R.drawable.ic_default_asset_logo)
                        }
                        onClick = { paymentMethodItem.clickAction() }
                    }
                }
            }
        }
    }
}
