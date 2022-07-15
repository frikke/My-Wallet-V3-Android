package piuk.blockchain.android.simplebuy.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentSimpleBuyCurrencySelectionBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyNavigator
import piuk.blockchain.android.simplebuy.SimpleBuyScreen
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class CurrencySelectionSheet :
    SlidingModalBottomDialog<FragmentSimpleBuyCurrencySelectionBinding>(),
    ChangeCurrencyOptionHost {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCurrencyChanged(currency: FiatCurrency, selectionType: CurrencySelectionType)
    }

    private val assetCatalogue: AssetCatalogue by inject()

    private val currencies: List<FiatCurrency> by unsafeLazy {
        arguments?.getStringArrayList(CURRENCIES_KEY)?.mapNotNull {
            assetCatalogue.fiatFromNetworkTicker(it)
        } ?: emptyList()
    }

    private val selectedCurrency: FiatCurrency by unsafeLazy {
        arguments?.getSerializable(SELECTED_CURRENCY) as FiatCurrency
    }

    private val selectionType: CurrencySelectionType by unsafeLazy {
        arguments?.getSerializable(SELECTION_TYPE) as CurrencySelectionType
    }

    private fun updateFiat(currency: FiatCurrency) {
        (host as? Host)?.onCurrencyChanged(currency, selectionType)
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
            introHeaderDescription.text = when {
                selectionType == CurrencySelectionType.DISPLAY_CURRENCY ->
                    getString(R.string.display_currency_selection_header)
                currencies.contains(selectedCurrency) -> getString(R.string.trading_currency_selection_header)
                else -> getString(R.string.currency_not_available, selectedCurrency.name)
            }

            introHeaderTitle.text = when (selectionType) {
                CurrencySelectionType.TRADING_CURRENCY -> getString(R.string.select_a_trading_currency)
                CurrencySelectionType.DISPLAY_CURRENCY -> getString(R.string.select_a_display_currency)
            }

            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = CurrenciesAdapter(
                showSectionDivider = true,
                items = currencies.sortedWith(compareBy { it.name })
            ) {
                updateFiat(it)
            }
        }
    }

    override fun navigator(): SimpleBuyNavigator = (activity as? SimpleBuyNavigator)
        ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun skip() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_SKIP)
        navigator().exitSimpleBuyFlow()
    }

    enum class CurrencySelectionType {
        TRADING_CURRENCY,
        DISPLAY_CURRENCY
    }

    companion object {
        private const val CURRENCIES_KEY = "CURRENCIES_KEY"
        private const val SELECTED_CURRENCY = "SELECTED_CURRENCY"
        private const val SELECTION_TYPE = "SELECTION_TYPE"

        fun newInstance(
            currencies: List<FiatCurrency> = emptyList(),
            selectedCurrency: FiatCurrency,
            currencySelectionType: CurrencySelectionType
        ): CurrencySelectionSheet {
            return CurrencySelectionSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(CURRENCIES_KEY, ArrayList(currencies.map { it.networkTicker }))
                    putSerializable(SELECTED_CURRENCY, selectedCurrency)
                    putSerializable(SELECTION_TYPE, currencySelectionType)
                }
            }
        }
    }
}

interface ChangeCurrencyOptionHost : SimpleBuyScreen {
    fun skip()
}
