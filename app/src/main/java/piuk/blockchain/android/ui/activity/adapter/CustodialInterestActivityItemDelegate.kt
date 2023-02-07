package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.getResolvedColor
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.Date
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.ActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.setTransactionHasFailed

class CustodialInterestActivityItemDelegate<in T>(
    private val currencyPrefs: CurrencyPrefs,
    private val historicRateFetcher: HistoricRateFetcher,
    private val onItemClicked: (AssetInfo, String, ActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CustodialInterestActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialInterestActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialInterestActivityItemViewHolder).bind(
        items[position] as CustodialInterestActivitySummaryItem,
        currencyPrefs.selectedFiatCurrency,
        historicRateFetcher,
        onItemClicked
    )
}

private class CustodialInterestActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: CustodialInterestActivitySummaryItem,
        selectedFiatCurrency: FiatCurrency,
        historicRateFetcher: HistoricRateFetcher,
        onAccountClicked: (AssetInfo, String, ActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            icon.setIcon(tx.isPending(), tx.type)
            when {
                tx.state.isPending().not() -> {
                    icon.setAssetIconColoursWithTint(tx.currency)
                }
                tx.state.hasFailed() -> icon.setTransactionHasFailed()
                else -> {
                    icon.background = null
                    icon.setColorFilter(Color.TRANSPARENT)
                }
            }

            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency, historicRateFetcher)

            txType.setTxLabel(tx.currency, tx.type)
            statusDate.setTxStatus(tx)
            setTextColours(tx.state)

            txRoot.setOnClickListener {
                onAccountClicked(tx.currency, tx.txId, ActivityType.CUSTODIAL_INTEREST)
            }
        }
    }

    private fun setTextColours(txStatus: EarnRewardsState) {
        with(binding) {
            if (txStatus.isCompleted()) {
                txType.setTextColor(context.getResolvedColor(R.color.black))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.black))
            } else {
                txType.setTextColor(context.getResolvedColor(R.color.grey_400))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.grey_400))
            }
        }
    }
}

private fun EarnRewardsState.isPending(): Boolean =
    this == EarnRewardsState.PENDING ||
        this == EarnRewardsState.PROCESSING ||
        this == EarnRewardsState.MANUAL_REVIEW

private fun EarnRewardsState.hasFailed(): Boolean =
    this == EarnRewardsState.FAILED

private fun EarnRewardsState.isCompleted(): Boolean =
    this == EarnRewardsState.COMPLETE

private fun ImageView.setIcon(txPending: Boolean, type: TransactionSummary.TransactionType) =
    setImageResource(
        if (txPending) {
            R.drawable.ic_tx_confirming
        } else {
            when (type) {
                TransactionSummary.TransactionType.DEPOSIT -> R.drawable.ic_tx_buy
                TransactionSummary.TransactionType.INTEREST_EARNED -> R.drawable.ic_tx_interest
                TransactionSummary.TransactionType.WITHDRAW -> R.drawable.ic_tx_sell
                else -> R.drawable.ic_tx_buy
            }
        }
    )

private fun TextView.setTxLabel(
    asset: AssetInfo,
    type: TransactionSummary.TransactionType
) {
    text = when (type) {
        TransactionSummary.TransactionType.DEPOSIT -> context.resources.getString(
            R.string.tx_title_added,
            asset.displayTicker
        )
        TransactionSummary.TransactionType.INTEREST_EARNED -> context.resources.getString(
            R.string.tx_title_rewards,
            asset.displayTicker
        )
        TransactionSummary.TransactionType.WITHDRAW -> context.resources.getString(
            R.string.tx_title_withdrawn,
            asset.displayTicker
        )
        else -> context.resources.getString(
            R.string.tx_title_transferred,
            asset.displayTicker
        )
    }
}

private fun TextView.setTxStatus(tx: CustodialInterestActivitySummaryItem) {
    text = when (tx.state) {
        EarnRewardsState.COMPLETE -> Date(tx.timeStampMs).toFormattedDate()
        EarnRewardsState.FAILED -> context.getString(R.string.activity_state_failed)
        EarnRewardsState.CLEARED -> context.getString(R.string.activity_state_cleared)
        EarnRewardsState.REFUNDED -> context.getString(R.string.activity_state_refunded)
        EarnRewardsState.PENDING -> context.getString(R.string.activity_state_pending)
        EarnRewardsState.PROCESSING -> context.getString(R.string.activity_state_pending)
        EarnRewardsState.MANUAL_REVIEW -> context.getString(R.string.activity_state_pending)
        EarnRewardsState.REJECTED -> context.getString(R.string.activity_state_rejected)
        EarnRewardsState.UNKNOWN -> context.getString(R.string.activity_state_unknown)
    }
}
