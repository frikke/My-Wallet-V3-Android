package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates
import piuk.blockchain.android.databinding.CurrencySelectionItemBinding

class CurrenciesAdapter(
    private val showSectionDivider: Boolean = false,
    private val onChecked: (CurrencyItem) -> Unit
) : RecyclerView.Adapter<CurrenciesAdapter.CurrenciesViewHolder>() {

    var items: List<CurrencyItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class CurrenciesViewHolder(binding: CurrencySelectionItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.name
        val symbol: TextView = binding.symbol
        val rootView: ViewGroup = binding.rootView
        val cellDivider: View = binding.cellDivider
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrenciesViewHolder =
        CurrenciesViewHolder(
            CurrencySelectionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CurrenciesViewHolder, position: Int) {

        with(holder) {
            val item = items[position]

            rootView.setOnClickListener {
                onChecked(item)
            }
            title.text = item.name
            symbol.text = item.symbol

            when {
                position == items.size - 1 && showSectionDivider -> {
                    cellDivider.visibility = View.GONE
                }
                position != items.size -> {
                    cellDivider.visibility = View.VISIBLE
                }
                else -> {
                    cellDivider.visibility = View.GONE
                }
            }
        }
    }
}
