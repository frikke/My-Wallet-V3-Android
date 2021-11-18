package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.koin.walletRedesignFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewIntroHeaderBinding
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.getResolvedDrawable
import piuk.blockchain.android.util.visibleIf

class IntroHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), KoinComponent {

    private val binding: ViewIntroHeaderBinding =
        ViewIntroHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    private val compositeDisposable = CompositeDisposable()
    private val redesignFeatureFlag: FeatureFlag by inject(walletRedesignFeatureFlag)

    init {
        setupView(context, attrs)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()
        super.onDetachedFromWindow()
    }

    private fun setupView(context: Context, attrs: AttributeSet?) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.IntroHeaderView, 0, 0)

        attributes?.let {
            val title = it.getString(R.styleable.IntroHeaderView_intro_header_title)
            val label = it.getString(R.styleable.IntroHeaderView_intro_header_label)
            val icon = it.getDrawable(R.styleable.IntroHeaderView_intro_header_icon)
            val showSeparator = it.getBoolean(R.styleable.IntroHeaderView_intro_header_separator, true)

            with(binding) {
                introHeaderTitle.text = title
                introHeaderLabel.text = label
                introHeaderIcon.setImageDrawable(icon)
                introHeaderSeparator.visibleIf { showSeparator }

                compositeDisposable += redesignFeatureFlag.enabled
                    .subscribeBy(
                        onSuccess = { enabled ->
                            if (enabled) {
                                introHeaderParent.setWhiteBackground()
                            } else {
                                try {
                                    introHeaderParent.background =
                                        context.getResolvedDrawable(R.drawable.ic_info_banner_background)
                                } catch (e: Resources.NotFoundException) {
                                    introHeaderParent.setWhiteBackground()
                                }
                            }
                        },
                        onError = {
                            introHeaderParent.setWhiteBackground()
                        }
                    )
            }
            attributes.recycle()
        }
    }

    private fun LinearLayout.setWhiteBackground() =
        setBackgroundColor(context.getResolvedColor(R.color.white))

    fun setDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int,
        showSeparator: Boolean = true
    ) {
        with(binding) {
            introHeaderTitle.text = context.getString(title)
            introHeaderLabel.text = context.getString(label)
            introHeaderIcon.setImageDrawable(context.getResolvedDrawable(icon))
            introHeaderSeparator.visibleIf { showSeparator }
        }
    }

    fun toggleBottomSeparator(visible: Boolean) {
        binding.introHeaderSeparator.visibleIf { visible }
    }
}
