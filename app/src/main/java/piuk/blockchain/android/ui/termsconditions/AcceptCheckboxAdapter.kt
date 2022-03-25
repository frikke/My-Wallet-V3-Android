package piuk.blockchain.android.ui.termsconditions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.control.CheckboxState
import piuk.blockchain.android.databinding.ItemTermsConditionsAcceptCheckboxBinding

class AcceptCheckboxAdapter(
    private val onChecked: (Boolean) -> Unit
) : RecyclerView.Adapter<AcceptCheckboxAdapter.VH>() {

    private var isChecked = false
    var isEnabled = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(
            ItemTermsConditionsAcceptCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            ::onCheckedStateChanged
        )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(isChecked, isEnabled)

    private fun onCheckedStateChanged(newIsChecked: Boolean) {
        if (isChecked == newIsChecked) return
        isChecked = newIsChecked
        notifyDataSetChanged()
        onChecked(newIsChecked)
    }

    class VH(
        private val binding: ItemTermsConditionsAcceptCheckboxBinding,
        private val onChecked: (Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(isChecked: Boolean, isEnabled: Boolean) = with(binding) {
            acceptCheckbox.onCheckChanged = {
                onChecked(it)
            }
            root.setOnClickListener { onChecked(!isChecked) }
            acceptCheckbox.state = if (isChecked) CheckboxState.Checked else CheckboxState.Unchecked
            acceptCheckbox.checkboxEnabled = isEnabled
        }
    }
}
