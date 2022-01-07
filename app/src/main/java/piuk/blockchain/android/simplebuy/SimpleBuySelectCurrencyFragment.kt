package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.wallet.api.data.Settings.Companion.UNIT_FIAT
import java.util.Currency
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentSimpleBuyCurrencySelectionBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuySelectCurrencyFragment :
    SlidingModalBottomDialog<FragmentSimpleBuyCurrencySelectionBinding>(),
    ChangeCurrencyOptionHost {

    private val currencyPrefs: CurrencyPrefs by inject()

    private val currencies: List<String> by unsafeLazy {
        arguments?.getStringArrayList(CURRENCIES_KEY) ?: emptyList()
    }

    private val selectedCurrency: String by unsafeLazy {
        arguments?.getString(SELECTED_CURRENCY).orEmpty()
    }

    // show all currencies if passed list is empty
    private var filter: (CurrencyItem) -> Boolean =
        { if (currencies.isEmpty()) true else currencies.contains(it.symbol) }

    private val adapter = CurrenciesAdapter(true) {
        updateFiat(it)
    }

    private fun updateFiat(it: CurrencyItem) {
        currencyPrefs.tradingCurrency = it.symbol
        dismiss()
    }

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSimpleBuyCurrencySelectionBinding =
        FragmentSimpleBuyCurrencySelectionBinding.inflate(inflater, container, false)

    override fun initControls(binding: FragmentSimpleBuyCurrencySelectionBinding) {
        analytics.logEvent(SimpleBuyAnalytics.SELECT_YOUR_CURRENCY_SHOWN)
        with(binding) {
            introHeaderDescription.text = getString(
                R.string.currency_not_available, Currency.getInstance(selectedCurrency).getDisplayName(locale)
            )
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = adapter
        }
        adapter.items = UNIT_FIAT.map { symbol ->
            CurrencyItem(
                name = Currency.getInstance(symbol).getDisplayName(locale),
                symbol = symbol
            )
        }.sortedWith(compareBy { it.name }).filter(filter)
    }

    private val locale = Locale.getDefault()

    override fun navigator(): SimpleBuyNavigator = (activity as? SimpleBuyNavigator)
        ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun needsToChange() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_CHANGE)
        adapter.items = adapter.items.filter(filter)
    }

    override fun skip() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_SKIP)
        navigator().exitSimpleBuyFlow()
    }

    companion object {
        private const val CURRENCIES_KEY = "CURRENCIES_KEY"
        private const val SELECTED_CURRENCY = "SELECTED_CURRENCY"
        fun newInstance(
            currencies: List<String> = emptyList(),
            selectedCurrency: String
        ): SimpleBuySelectCurrencyFragment {
            return SimpleBuySelectCurrencyFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(CURRENCIES_KEY, ArrayList(currencies))
                    putString(SELECTED_CURRENCY, selectedCurrency)
                }
            }
        }
    }
}

data class CurrencyItem(
    val name: String,
    val symbol: String
)

interface ChangeCurrencyOptionHost : SimpleBuyScreen {
    fun needsToChange()
    fun skip()
}
