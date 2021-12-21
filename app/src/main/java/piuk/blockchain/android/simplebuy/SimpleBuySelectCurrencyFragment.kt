package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentSimpleBuyCurrencySelectionBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuySelectCurrencyFragment :
    SlidingModalBottomDialog<FragmentSimpleBuyCurrencySelectionBinding>(),
    ChangeCurrencyOptionHost {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCurrencyChanged()
    }

    private val currencyPrefs: CurrencyPrefs by inject()
    private val assetCatalogue: AssetCatalogue by inject()

    private val currencies: List<FiatCurrency> by unsafeLazy {
        arguments?.getStringArrayList(CURRENCIES_KEY)?.mapNotNull {
            assetCatalogue.fiatFromNetworkTicker(it)
        } ?: emptyList()
    }

    private val selectedCurrency: FiatCurrency by unsafeLazy {
        arguments?.getSerializable(SELECTED_CURRENCY) as FiatCurrency
    }

    private val adapter = CurrenciesAdapter(true) {
        updateFiat(it)
    }

    private fun updateFiat(it: CurrencyItem) {
        currencyPrefs.tradingCurrency =
            assetCatalogue.fiatFromNetworkTicker(it.symbol) ?: throw IllegalStateException("Unknown fiat currency")
        (host as? Host)?.onCurrencyChanged()
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
                R.string.currency_not_available, selectedCurrency.name
            )
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = adapter
            adapter.items = currencies.map { currency ->
                CurrencyItem(
                    name = currency.name,
                    symbol = currency.symbol
                )
            }.sortedWith(compareBy { it.name })
        }
    }

    private val locale = Locale.getDefault()

    override fun navigator(): SimpleBuyNavigator = (activity as? SimpleBuyNavigator)
        ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun skip() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_SKIP)
        navigator().exitSimpleBuyFlow()
    }

    companion object {
        private const val CURRENCIES_KEY = "CURRENCIES_KEY"
        private const val SELECTED_CURRENCY = "SELECTED_CURRENCY"
        fun newInstance(
            currencies: List<FiatCurrency> = emptyList(),
            selectedCurrency: FiatCurrency
        ): SimpleBuySelectCurrencyFragment {
            return SimpleBuySelectCurrencyFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(CURRENCIES_KEY, ArrayList(currencies.map { it.networkTicker }))
                    putSerializable(SELECTED_CURRENCY, selectedCurrency)
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
    fun skip()
}
