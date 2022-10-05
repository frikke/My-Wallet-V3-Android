package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemDashboardBalanceCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.model.BrokerageBalanceState
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor

class BalanceCardDelegate(
    private val assetResources: AssetResources,
    private val walletModeService: WalletModeService
) : AdapterDelegate<DashboardItem> {

    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is BrokerageBalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BalanceCardViewHolder(
            binding = ItemDashboardBalanceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            assetResources = assetResources,
            walletMode = walletModeService.enabledWalletMode()
        )

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
    ) = (holder as BalanceCardViewHolder).bind(
        items[position] as BrokerageBalanceState,
    )
}

private class BalanceCardViewHolder(
    private val binding: ItemDashboardBalanceCardBinding,
    private val assetResources: AssetResources,
    private val walletMode: WalletMode
) : RecyclerView.ViewHolder(binding.root) {

    private var isFirstLoad = true

    fun bind(state: BrokerageBalanceState) {
        configurePieChart()

        if (state.isLoading) {
            renderLoading()
        } else {
            renderLoaded(state)
        }
    }

    private fun renderLoading() {
        with(binding) {
            if (isFirstLoad) {
                totalBalance.resetLoader()
                balanceDeltaValue.resetLoader()
                balanceDeltaPercent.resetLoader()
            }
        }
        populateEmptyPieChart()
        isFirstLoad = false
    }

    @SuppressLint("SetTextI18n")
    private fun renderLoaded(state: BrokerageBalanceState) {

        with(binding) {
            totalBalance.text = state.fiatBalance?.toStringWithSymbol().orEmpty()
            label.text =
                if (walletMode == WalletMode.UNIVERSAL) context.getString(R.string.dashboard_total_balance)
                else context.getString(R.string.common_balance)
            if (state.delta == null) {
                balanceDeltaValue.text = ""
                balanceDeltaPercent.text = ""
            } else {
                val (deltaVal, deltaPercent) = state.delta!!
                balanceDeltaValue.text = deltaVal.toStringWithSymbol()
                balanceDeltaValue.setDeltaColour(deltaPercent)
                balanceDeltaPercent.asDeltaPercent(deltaPercent, "(", ")")
            }

            populatePieChart(state)
            isFirstLoad = true
        }
    }

    private fun populateEmptyPieChart() {
        with(binding) {
            val entries = listOf(PieEntry(100f))

            val sliceColours = listOf(context.getResolvedColor(R.color.grey_100))

            pieChart.data = PieData(
                PieDataSet(entries, null).apply {
                    sliceSpace = 5f
                    setDrawIcons(false)
                    setDrawValues(false)
                    colors = sliceColours
                }
            )
            pieChart.invalidate()
        }
    }

    private fun populatePieChart(state: BrokerageBalanceState) {
        with(binding) {
            val assets = state.assetList

            val entries = ArrayList<PieEntry>().apply {
                assets.forEach { assetState ->
                    val point = assetState.fiatBalance?.toFloat() ?: 0f
                    add(PieEntry(point))
                }
            }

            if (entries.all { it.value == 0.0f }) {
                populateEmptyPieChart()
            } else {
                val sliceColours = assets.map {
                    assetResources.assetColor(it.currency)
                }

                pieChart.data = PieData(
                    PieDataSet(entries, null).apply {
                        sliceSpace = SLICE_SPACE_DP
                        setDrawIcons(false)
                        setDrawValues(false)
                        colors = sliceColours
                    }
                )
                pieChart.invalidate()
            }
        }
    }

    private fun configurePieChart() {
        with(binding.pieChart) {
            setDrawCenterText(false)

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = PIE_HOLE_RADIUS

            setDrawEntryLabels(false)
            legend.isEnabled = false
            description.isEnabled = false

            setTouchEnabled(false)
            setNoDataText(null)
        }
    }

    companion object {
        private const val SLICE_SPACE_DP = 2f
        private const val PIE_HOLE_RADIUS = 85f
    }
}
