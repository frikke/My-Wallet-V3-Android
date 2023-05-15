package piuk.blockchain.android.ui.debug

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.FeatureFlagState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemDebuggerFeatureFlagBinding
import piuk.blockchain.android.util.context

data class FeatureFlagItem(
    val name: String,
    val featureFlagState: FeatureFlagState,
    val onStatusChanged: (FeatureFlagState) -> Unit
)

class FeatureFlagAdapter : RecyclerView.Adapter<FeatureFlagViewHolder>() {

    var items: List<FeatureFlagItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureFlagViewHolder {
        return FeatureFlagViewHolder(
            ItemDebuggerFeatureFlagBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FeatureFlagViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

class FeatureFlagViewHolder(
    private val binding: ItemDebuggerFeatureFlagBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FeatureFlagItem) {
        with(binding) {
            titleView.text = item.name
            var firstSelection = true

            val adapter = FeatureFlagSpinnerAdapter(
                context = context,
                resource = android.R.layout.simple_spinner_item,
                objects = FeatureFlagState.values().map { it.toString() },
                icons = FeatureFlagState.values().map { it.getIcon() }
            )
            adapter.setDropDownViewResource(com.veriff.R.layout.support_simple_spinner_dropdown_item)
            root.setOnClickListener { spinner.performClick() }
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!firstSelection) {
                        item.onStatusChanged(FeatureFlagState.values()[position])
                    } else {
                        firstSelection = false
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
            spinner.setSelection(FeatureFlagState.values().indexOf(item.featureFlagState))
        }
    }

    private fun FeatureFlagState.getIcon() = when (this) {
        FeatureFlagState.ENABLED -> R.drawable.ic_debugger_featureflag_on
        FeatureFlagState.DISABLED -> R.drawable.ic_debugger_featureflag_off
        FeatureFlagState.REMOTE -> R.drawable.ic_debugger_featureflag_default
    }
}

class FeatureFlagSpinnerAdapter(
    context: Context,
    resource: Int,
    objects: List<String>,
    private val icons: List<Int>
) : ArrayAdapter<String>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            if (this is TextView) {
                text = ""
                setCompoundDrawablesWithIntrinsicBounds(if (icons.size > position) icons[position] else 0, 0, 0, 0)
            }
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            if (this is TextView) {
                setCompoundDrawablesWithIntrinsicBounds(if (icons.size > position) icons[position] else 0, 0, 0, 0)
            }
        }
    }
}
