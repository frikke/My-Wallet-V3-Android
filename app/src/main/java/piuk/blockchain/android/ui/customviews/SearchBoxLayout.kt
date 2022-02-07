package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.blockchain.componentlib.viewextensions.visibleIf
import com.google.android.material.textfield.TextInputLayout
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SearchBoxLayoutBinding

class SearchBoxLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextInputLayout(context, attrs, defStyleAttr) {

    private val binding: SearchBoxLayoutBinding =
        SearchBoxLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun setDetails(@StringRes hint: Int, textWatcher: TextWatcher) {
        with(binding) {
            root.hint = context.resources.getString(hint)
            searchEditText.addTextChangedListener(textWatcher)
        }
    }

    fun updateResults(resultCount: String, shouldShow: Boolean) {
        binding.searchResultsLabel.apply {
            visibleIf { shouldShow }
            text = context.resources.getString(R.string.search_results, resultCount)
        }
    }

    fun updateLayoutState() {
        with(binding) {
            if (searchEditText.text?.isNotEmpty() == true) {
                root.setEndIconDrawable(R.drawable.ic_close)
                root.setEndIconOnClickListener { searchEditText.text?.clear() }
            } else {
                root.setEndIconOnClickListener {}
                root.setEndIconDrawable(R.drawable.ic_search)
            }
        }
    }
}
