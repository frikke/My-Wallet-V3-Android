package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemDashboardBalanceCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.model.BalanceState
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor

class BalanceCardDelegate<in T>(
    private val selectedFiat: FiatCurrency,
    private val assetResources: AssetResources,
    private val enabledWalletModeService: WalletModeService,
    private val onWalletModeChangeClicked: () -> Unit,
) : AdapterDelegate<T> {

    private val enabledWalletMode: WalletMode
        get() = enabledWalletModeService.enabledWalletMode()

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is BalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BalanceCardViewHolder(
            binding = ItemDashboardBalanceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            selectedFiat = selectedFiat,
            assetResources = assetResources,
            onWalletModeChangeClicked = onWalletModeChangeClicked,
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
    ) = (holder as BalanceCardViewHolder).bind(
        items[position] as BalanceState,
        enabledWalletMode
    )
}

private class BalanceCardViewHolder(
    private val binding: ItemDashboardBalanceCardBinding,
    private val selectedFiat: FiatCurrency,
    private val assetResources: AssetResources,
    private val onWalletModeChangeClicked: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var isFirstLoad = true

    fun bind(state: BalanceState, activeWalletMode: WalletMode) {
        configurePieChart()
        configureActiveWalletMode(activeWalletMode)

        if (state.isLoading) {
            renderLoading()
        } else {
            renderLoaded(state)
        }
    }

    private fun configureActiveWalletMode(walletMode: WalletMode) {
        with(binding) {
            modeSelectorUi.visibleIf { walletMode != WalletMode.UNIVERSAL }
            modeSelectorUi.setOnClickListener { onWalletModeChangeClicked() }
            walletModeIcon.setImageResource(
                if (walletMode == WalletMode.CUSTODIAL_ONLY) {
                    R.drawable.ic_portfolio
                } else R.drawable.ic_defi_wallet
            )
            walletModeLabel.text = when (walletMode) {
                WalletMode.NON_CUSTODIAL_ONLY -> context.getString(R.string.defi)
                WalletMode.CUSTODIAL_ONLY -> context.getString(R.string.brokerage)
                WalletMode.UNIVERSAL -> context.getString(R.string.empty)
            }
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
    private fun renderLoaded(state: BalanceState) {

        with(binding) {
            totalBalance.text = state.fiatBalance?.toStringWithSymbol().orEmpty()
            label.text = context.getString(R.string.dashboard_total_balance)

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

    private fun populatePieChart(state: BalanceState) {
        with(binding) {
            val assets = state.assetList

            val entries = ArrayList<PieEntry>().apply {
                assets.forEach { assetState ->
                    val point = assetState.fiatBalance?.toFloat() ?: 0f
                    add(PieEntry(point))
                }

                // Add all fiat from Funds
                add(PieEntry(state.getFundsFiat(selectedFiat).toFloat()))
            }

            if (entries.all { it.value == 0.0f }) {
                populateEmptyPieChart()
            } else {
                val sliceColours = assets.map {
                    assetResources.assetColor(it.currency)
                }.toMutableList()

                // Add colour for Funds
                sliceColours.add(ContextCompat.getColor(itemView.context, R.color.green_500))

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
