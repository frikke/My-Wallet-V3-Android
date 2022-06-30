package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.FiatAccount
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import piuk.blockchain.android.databinding.ItemDashboardFundsBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsBorderedBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsParentBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.FiatAssetState
import piuk.blockchain.android.ui.dashboard.model.FiatBalanceInfo

class FundsCardDelegate(
    private val selectedFiat: Currency,
    private val onFundsItemClicked: (FiatAccount) -> Unit
) : AdapterDelegate<DashboardItem> {

    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is FiatAssetState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemDashboardFundsParentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FundsCardViewHolder(
            binding,
            ItemDashboardFundsBinding.inflate(LayoutInflater.from(parent.context), binding.root, true),
            onFundsItemClicked,
            selectedFiat
        )
    }

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FiatAssetState)
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

    fun bind(funds: FiatAssetState) {
        if (funds.fiatAccounts.size == 1) {
            showSingleAsset(funds.fiatAccounts.values.first())
        } else {
            with(binding) {
                fundsSingleItem.gone()
                fundsList.apply {
                    layoutManager =
                        LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = multipleFundsAdapter
                }
                multipleFundsAdapter.items = funds.fiatAccounts.values.toList()
            }
        }
    }

    private fun showSingleAsset(assetInfo: FiatBalanceInfo) {
        val ticker = assetInfo.account.currency.networkTicker
        singleLayoutBinding.apply {
            fundsUserFiatBalance.visibleIf { selectedFiat.networkTicker != ticker }
            fundsUserFiatBalance.text = assetInfo.balance.toStringWithSymbol()
            binding.fundsList.gone()
            binding.fundsSingleItem.setOnClickListener {
                onFundsItemClicked(assetInfo.account)
            }
            fundsTitle.text = assetInfo.account.currency.name
            fundsFiatTicker.text = ticker
            fundsBalance.text = if (selectedFiat.networkTicker == ticker) {
                assetInfo.balance.toStringWithSymbol()
            } else {
                val fiat = assetInfo.userFiat ?: Money.zero(selectedFiat)
                fiat.toStringWithSymbol()
            }
            fundsIcon.setIcon(assetInfo.account.currency)
        }
    }
}

private class MultipleFundsAdapter(
    private val onFundsItemClicked: (FiatAccount) -> Unit,
    private val selectedFiat: Currency
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<FiatBalanceInfo>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SingleFundsViewHolder(
            ItemDashboardFundsBorderedBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFundsItemClicked, selectedFiat
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

        fun bind(assetInfo: FiatBalanceInfo) {
            val ticker = assetInfo.account.currency.networkTicker
            binding.apply {
                borderedFundsBalanceOtherFiat.visibleIf { selectedFiat.networkTicker != ticker }
                borderedFundsBalanceOtherFiat.text = assetInfo.balance.toStringWithSymbol()

                borderedFundsParent.setOnClickListener {
                    onFundsItemClicked(assetInfo.account)
                }
                borderedFundsTitle.text = assetInfo.account.currency.name
                borderedFundsFiatTicker.text = ticker
                borderedFundsBalance.text = if (selectedFiat.networkTicker == ticker) {
                    assetInfo.balance.toStringWithSymbol()
                } else {
                    assetInfo.userFiat?.toStringWithSymbol()
                }
                borderedFundsIcon.setIcon(assetInfo.account.currency)
            }
        }
    }
}
