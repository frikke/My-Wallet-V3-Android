package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.FiatAccount
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.databinding.ItemDashboardFundsBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsBorderedBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsParentBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.BrokerageFiatAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.FiatBalanceInfo

class FundsCardDelegate(
    private val selectedFiat: Currency,
    private val onFundsItemClicked: (FiatAccount) -> Unit
) : AdapterDelegate<DashboardItem> {

    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is FiatBalanceInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemDashboardFundsParentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FundsCardViewHolder(
            binding,
            binding.itemDashboardFunds,
            onFundsItemClicked,
            selectedFiat
        )
    }

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FiatBalanceInfo)
}

private class FundsCardViewHolder(
    private val binding: ItemDashboardFundsParentBinding,
    private val singleLayoutBinding: ItemDashboardFundsBinding,
    private val onFundsItemClicked: (FiatAccount) -> Unit,
    private val selectedFiat: Currency
) : RecyclerView.ViewHolder(binding.root) {

    private val multipleFundsAdapter: MultipleFundsAdapter by lazy {
        MultipleFundsAdapter(onFundsItemClicked, selectedFiat)
    }

    fun bind(fiats: FiatBalanceInfo) {
        binding.fundsSingleItem.visibleIf { fiats.isSingleCurrency }
        binding.fundsList.visibleIf { !fiats.isSingleCurrency }

        if (fiats.isSingleCurrency) {
            val item = fiats.funds[0]
            singleLayoutBinding.apply {
                fundsUserFiatBalance.visibleIf { selectedFiat != item.currency }
                fundsUserFiatBalance.text = item.fiatBalance(
                    useDisplayBalance = item.assetDisplayBalanceFFEnabled
                )?.toStringWithSymbol()
                binding.fundsSingleItem.setOnClickListener {
                    onFundsItemClicked(item.fiatAccount)
                }
                fundsTitle.text = item.currency.name
                fundsFiatTicker.text = item.currency.displayTicker
                fundsBalance.text =
                    item.accountBalance?.total?.toStringWithSymbol()

                fundsIcon.setIcon(item.currency as FiatCurrency)
            }
        } else {
            with(binding) {
                fundsList.apply {
                    layoutManager =
                        LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = multipleFundsAdapter
                }
                multipleFundsAdapter.items = fiats.funds
            }
        }
    }
}

private class MultipleFundsAdapter(
    private val onFundsItemClicked: (FiatAccount) -> Unit,
    private val selectedFiat: Currency
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<BrokerageFiatAsset>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SingleFundsViewHolder(
            ItemDashboardFundsBorderedBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFundsItemClicked,
            selectedFiat
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = (holder as SingleFundsViewHolder).bind(items[position])

    private class SingleFundsViewHolder(
        private val binding: ItemDashboardFundsBorderedBinding,
        private val onFundsItemClicked: (FiatAccount) -> Unit,
        private val selectedFiat: Currency
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BrokerageFiatAsset) {
            val currency = item.currency as FiatCurrency
            binding.apply {
                borderedFundsBalanceOtherFiat.visibleIf { selectedFiat != currency }
                borderedFundsBalanceOtherFiat.text = item.accountBalance?.total?.toStringWithSymbol()
                borderedFundsParent.setOnClickListener {
                    onFundsItemClicked(item.fiatAccount)
                }
                borderedFundsTitle.text = currency.name
                borderedFundsFiatTicker.text = currency.displayTicker
                borderedFundsBalance.text = item.fiatBalance(
                    useDisplayBalance = item.assetDisplayBalanceFFEnabled
                )?.toStringWithSymbol()
                borderedFundsIcon.setIcon(currency)
            }
        }
    }
}
