package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.getResolvedDrawable
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewIntroHeaderBinding

class IntroHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), KoinComponent {

    private val binding: ViewIntroHeaderBinding =
        ViewIntroHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupView(context, attrs)
    }

    private fun setupView(context: Context, attrs: AttributeSet?) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.IntroHeaderView, 0, 0)

        attributes?.let {
            val title = it.getString(R.styleable.IntroHeaderView_intro_header_title)
            val icon = it.getDrawable(R.styleable.IntroHeaderView_intro_header_icon)
            val background = it.getDrawable(R.styleable.IntroHeaderView_intro_header_background)
            val showSeparator = it.getBoolean(R.styleable.IntroHeaderView_intro_header_separator, true)

            with(binding) {
                introHeaderTitle.text = title
                introHeaderIcon.setImageDrawable(icon)
                introHeaderParent.background = background
                introHeaderSeparator.visibleIf { showSeparator }
            }
            attributes.recycle()
        }
    }

    fun setDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int,
        @ColorRes background: Int = R.color.white,
        showSeparator: Boolean = true
    ) {
        with(binding) {
            introHeaderTitle.text = context.getString(title)
            introHeaderLabel.text = context.getString(label)
            introHeaderIcon.setImageDrawable(context.getResolvedDrawable(icon))
            introHeaderParent.background = context.getResolvedDrawable(background)
            introHeaderSeparator.visibleIf { showSeparator }
        }
    }

    fun toggleBottomSeparator(visible: Boolean) {
        binding.introHeaderSeparator.visibleIf { visible }
    }
}
