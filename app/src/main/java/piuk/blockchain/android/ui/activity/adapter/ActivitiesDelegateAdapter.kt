package piuk.blockchain.android.ui.activity.adapter

import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CryptoActivitySummaryItem
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.activity.ActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import timber.log.Timber

class ActivitiesDelegateAdapter(
    prefs: CurrencyPrefs,
    historicRateFetcher: HistoricRateFetcher,
    onItemClicked: (Currency, String, ActivityType) -> Unit,
) : DelegationAdapter<ActivitySummaryItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<ActivitySummaryItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(ActivityItemsDiffUtils(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(NonCustodialActivityItemDelegate(prefs, historicRateFetcher, onItemClicked))
            addAdapterDelegate(SwapActivityItemDelegate(onItemClicked))
            addAdapterDelegate(CustodialTradingActivityItemDelegate(onItemClicked))
            addAdapterDelegate(SellActivityItemDelegate(onItemClicked))
            addAdapterDelegate(CustodialFiatActivityItemDelegate(prefs, onItemClicked))
            addAdapterDelegate(CustodialInterestActivityItemDelegate(prefs, historicRateFetcher, onItemClicked))
            addAdapterDelegate(CustodialRecurringBuyActivityItemDelegate(onItemClicked))
            addAdapterDelegate(CustodialSendActivityItemDelegate(onItemClicked))
        }
    }
}

fun TextView.bindAndConvertFiatBalance(
    tx: CryptoActivitySummaryItem,
    disposables: CompositeDisposable,
    selectedFiatCurrency: FiatCurrency,
    historicRateFetcher: HistoricRateFetcher,
) {
    disposables += historicRateFetcher.fetch(tx.asset, selectedFiatCurrency, tx.timeStampMs, tx.value)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(
            onSuccess = {
                text = it.toStringWithSymbol()
                visible()
            },
            onError = {
                Timber.e("Cannot convert to fiat")
            }
        )
}

private class ActivityItemsDiffUtils(
    private val oldItems: List<ActivitySummaryItem>,
    private val newItems: List<ActivitySummaryItem>,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].txId == newItems[newItemPosition].txId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldItems[oldItemPosition].txId == newItems[newItemPosition].txId &&
            oldItems[oldItemPosition].stateIsFinalised &&
            newItems[newItemPosition].stateIsFinalised
}
