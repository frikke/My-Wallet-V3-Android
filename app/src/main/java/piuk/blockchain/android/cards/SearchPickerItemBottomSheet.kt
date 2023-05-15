package piuk.blockchain.android.cards

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.fragment.app.DialogFragment
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.utils.unsafeLazy
import java.io.Serializable
import piuk.blockchain.android.R

class SearchPickerItemBottomSheet : ComposeModalBottomDialog() {

    override val host: PickerItemListener
        get() = super.host as PickerItemListener

    private val items: List<PickerItem> by unsafeLazy {
        (requireArguments().getSerializable(ARG_PICKER_ITEMS) as? List<PickerItem>) ?: emptyList()
    }

    private val suggestedPick: PickerItem? by unsafeLazy {
        requireArguments().getSerializable(ARG_SUGGESTED_PICK) as? PickerItem
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, com.blockchain.common.R.style.FloatingBottomSheet)
    }

    @Composable
    override fun Sheet() {
        SearchPickerItemScreen(
            suggestedPick = suggestedPick,
            items = items,
            onItemClicked = {
                host.onItemPicked(it)
                dismiss()
            }
        )
    }

    companion object {
        private const val ARG_PICKER_ITEMS = "ARG_PICKER_ITEMS"
        private const val ARG_SUGGESTED_PICK = "ARG_SUGGESTED_PICK"
        fun newInstance(items: List<PickerItem>, suggestedPick: PickerItem? = null): SearchPickerItemBottomSheet =
            SearchPickerItemBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(ARG_PICKER_ITEMS, items as Serializable)
                    it.putSerializable(ARG_SUGGESTED_PICK, suggestedPick)
                }
            }
    }
}

interface PickerItem : Serializable {
    val label: String
    val code: String
    val icon: String?
}

interface PickerItemListener : ComposeModalBottomDialog.Host {
    fun onItemPicked(item: PickerItem)
}
