package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.earn.domain.models.staking.StakingState
import com.blockchain.preferences.CurrencyPrefs
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
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.setTransactionHasFailed

class CustodialStakingActivityItemDelegate<in T>(
    private val currencyPrefs: CurrencyPrefs,
    private val historicRateFetcher: HistoricRateFetcher,
    private val onItemClicked: (AssetInfo, String, ActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CustodialStakingActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialStakingActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialStakingActivityItemViewHolder).bind(
        items[position] as CustodialStakingActivitySummaryItem,
        currencyPrefs.selectedFiatCurrency,
        historicRateFetcher,
        onItemClicked
    )
}

private class CustodialStakingActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: CustodialStakingActivitySummaryItem,
        selectedFiatCurrency: FiatCurrency,
        historicRateFetcher: HistoricRateFetcher,
        onAccountClicked: (AssetInfo, String, ActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            icon.setIcon(tx.isPending(), tx.type)
            when {
                tx.status.isPending().not() -> {
                    icon.setAssetIconColoursWithTint(tx.asset)
                }
                tx.status.hasFailed() -> icon.setTransactionHasFailed()
                else -> {
                    icon.background = null
                    icon.setColorFilter(Color.TRANSPARENT)
                }
            }

            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency, historicRateFetcher)

            txType.setTxLabel(tx.asset, tx.type)
            statusDate.setTxStatus(tx)
            setTextColours(tx.status)

            txRoot.setOnClickListener {
                onAccountClicked(tx.asset, tx.txId, ActivityType.CUSTODIAL_STAKING)
            }
        }
    }

    private fun setTextColours(txStatus: StakingState) {
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

private fun StakingState.isPending(): Boolean =
    this == StakingState.PENDING ||
        this == StakingState.PROCESSING ||
        this == StakingState.MANUAL_REVIEW

private fun StakingState.hasFailed(): Boolean =
    this == StakingState.FAILED

private fun StakingState.isCompleted(): Boolean =
    this == StakingState.COMPLETE

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
            R.string.tx_title_staked,
            asset.displayTicker
        )
        TransactionSummary.TransactionType.INTEREST_EARNED -> context.resources.getString(
            R.string.tx_title_stake_earned,
            asset.displayTicker
        )
        TransactionSummary.TransactionType.WITHDRAW -> context.resources.getString(
            R.string.tx_title_stake_withdrawn,
            asset.displayTicker
        )
        else -> context.resources.getString(
            R.string.tx_title_transferred,
            asset.displayTicker
        )
    }
}

private fun TextView.setTxStatus(tx: CustodialStakingActivitySummaryItem) {
    text = when (tx.status) {
        StakingState.COMPLETE -> Date(tx.timeStampMs).toFormattedDate()
        StakingState.FAILED -> context.getString(R.string.activity_state_failed)
        StakingState.CLEARED -> context.getString(R.string.activity_state_cleared)
        StakingState.REFUNDED -> context.getString(R.string.activity_state_refunded)
        StakingState.PENDING -> context.getString(R.string.activity_state_pending)
        StakingState.PROCESSING -> context.getString(R.string.activity_state_pending)
        StakingState.MANUAL_REVIEW -> context.getString(R.string.activity_state_pending)
        StakingState.REJECTED -> context.getString(R.string.activity_state_rejected)
        StakingState.UNKNOWN -> context.getString(R.string.activity_state_unknown)
    }
}
