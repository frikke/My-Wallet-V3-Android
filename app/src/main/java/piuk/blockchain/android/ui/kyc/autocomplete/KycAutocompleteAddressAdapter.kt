package piuk.blockchain.android.ui.kyc.autocomplete

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ViewSearchResultBinding

data class KycAddressResult(val text: String, val searchText: String, val placeId: String)

class KycAutocompleteAddressAdapter(
    diffCallback: DiffUtil.ItemCallback<KycAddressResult> = object :
        DiffUtil.ItemCallback<KycAddressResult>() {
        override fun areItemsTheSame(
            oldItem: KycAddressResult,
            newItem: KycAddressResult
        ): Boolean {
            return oldItem == newItem
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: KycAddressResult,
            newItem: KycAddressResult
        ): Boolean {
            return oldItem.placeId == newItem.placeId && oldItem.searchText == newItem.searchText
        }
    },
    val onClick: (KycAddressResult) -> Unit
) : ListAdapter<KycAddressResult, KycAutocompleteAddressAdapter.AddressViewHolder>(diffCallback) {

    class AddressViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        return AddressViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.view_search_result, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val item = getItem(position)
        val itemBinding = ViewSearchResultBinding.bind(holder.itemView)

        itemBinding.root.setOnClickListener {
            onClick(item)
        }

        val startIndex = item.text.indexOf(item.searchText, ignoreCase = true)

        val spannable = SpannableString(item.text)
        if (startIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(itemBinding.root.context.getColor(R.color.paletteBaseTextTitle)), startIndex,
                startIndex + item.searchText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD), startIndex, startIndex + item.searchText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        itemBinding.searchResultText.text = spannable
    }
}