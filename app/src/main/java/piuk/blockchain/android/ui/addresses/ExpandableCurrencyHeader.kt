package piuk.blockchain.android.ui.addresses

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.MultipleWalletsAsset
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import com.blockchain.presentation.getResolvedDrawable
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Locale
import kotlinx.coroutines.rx3.asObservable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewExpandingCurrencyHeaderBinding
import piuk.blockchain.android.util.setAnimationListener

class ExpandableCurrencyHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs), KoinComponent {

    fun interface ExpandableCurrencyHeaderAnimationListener {
        fun onHeaderAnimationEnd(isOpen: Boolean)
    }

    private var animationListener: ExpandableCurrencyHeaderAnimationListener? = null

    private lateinit var selectionListener: (AssetInfo) -> Unit

    private val analytics: Analytics by inject()
    private val coincore: Coincore by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    private var expanded = false
    private var firstOpen = true
    private var collapsedHeight: Int = 0
    private var contentHeight: Int = 0
    private var contentWidth: Int = 0
    private var selectedCurrency: AssetInfo = CryptoCurrency.BTC

    private val arrowDrawable: Drawable? by unsafeLazy {
        VectorDrawableCompat.create(
            resources,
            R.drawable.vector_expand_more,
            ContextThemeWrapper(context, com.blockchain.common.R.style.AppTheme).theme
        )?.run {
            DrawableCompat.wrap(this)
        }
    }

    private val binding: ViewExpandingCurrencyHeaderBinding by lazy {
        ViewExpandingCurrencyHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    }

    init {
        // Inflate layout
        compositeDisposable += coincore.activeAssets().asObservable().firstOrError()
            .map { it.filterIsInstance<MultipleWalletsAsset>() }
            .map { it.map { asset -> asset.currency } }
            .subscribeBy { assets ->
                assets.forEach { asset ->
                    redesignTextView(asset)?.apply {
                        setOnClickListener { closeLayout(asset) }
                    }
                }
            }

        binding.textviewSelectedCurrency.apply {
            // Hide selector on first load
            invisible()
            setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        with(binding) {
            linearLayoutCoinSelection.invisible()
            textviewSelectedCurrency.setOnClickListener { animateLayout(true) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        with(binding) {
            contentFrame.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            textviewSelectedCurrency.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec)
            collapsedHeight = textviewSelectedCurrency.measuredHeight
            contentWidth = contentFrame.measuredWidth + textviewSelectedCurrency.measuredWidth
            contentHeight = contentFrame.measuredHeight

            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            if (firstOpen) {
                contentFrame.layoutParams.width = contentWidth
                contentFrame.layoutParams.height = collapsedHeight
                firstOpen = false
            }

            val width = textviewSelectedCurrency.measuredWidth + contentFrame.measuredWidth
            val height = contentFrame.measuredHeight

            setMeasuredDimension(width, height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = CustomOutline(w, h)
    }

    fun setAnimationListener(animationListener: ExpandableCurrencyHeaderAnimationListener) {
        this.animationListener = animationListener
    }

    fun setSelectionListener(selectionListener: (AssetInfo) -> Unit) {
        this.selectionListener = selectionListener
    }

    fun setCurrentlySelectedCurrency(asset: AssetInfo) {
        selectedCurrency = asset
        updateCurrencyUi(selectedCurrency)
    }

    private fun redesignTextView(asset: AssetInfo): TextView? =
        when (asset) {
            CryptoCurrency.BTC -> binding.textviewBitcoinRedesign
            CryptoCurrency.BCH -> binding.textviewBitcoinCashRedesign
            else -> null
        }

    fun isOpen() = expanded

    fun close() {
        if (isOpen()) closeLayout(null)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()
        super.onDetachedFromWindow()
    }

    private fun animateLayout(expanding: Boolean) {
        with(binding) {
            if (expanding) {
                textviewSelectedCurrency.setOnClickListener(null)
                val animation = AlphaAnimation(1.0f, 0.0f).apply { duration = 250 }
                animation.setAnimationListener {
                    onAnimationEnd {
                        textviewSelectedCurrency.alpha = 0.0f
                        startContentAnimation()
                    }
                }
                textviewSelectedCurrency.startAnimation(animation)
            } else {
                textviewSelectedCurrency.setOnClickListener { animateLayout(true) }
                startContentAnimation()
            }
        }
    }

    private fun startContentAnimation() {
        val animation: Animation = if (expanded) {
            binding.linearLayoutCoinSelection.invisible()
            ExpandAnimation(contentHeight, collapsedHeight)
        } else {
            this@ExpandableCurrencyHeader.invalidate()
            ExpandAnimation(collapsedHeight, contentHeight)
        }

        animation.duration = 250
        animation.setAnimationListener {
            onAnimationEnd {
                expanded = !expanded

                animationListener?.onHeaderAnimationEnd(isOpen = expanded)

                if (expanded) {
                    analytics.logEvent(AnalyticsEvents.OpenAssetsSelector)
                    binding.linearLayoutCoinSelection.visible()
                } else {
                    analytics.logEvent(AnalyticsEvents.CloseAssetsSelector)
                }
            }
        }
        binding.contentFrame.startAnimation(animation)
    }

    private fun updateCurrencyUi(asset: AssetInfo) {
        binding.textviewSelectedCurrency.run {
            text = asset.name.toUpperCase(Locale.ROOT)

            setCompoundDrawablesWithIntrinsicBounds(
                context.getResolvedDrawable(coinIcon(asset)),
                null,
                arrowDrawable,
                null
            )

            visible()
        }
    }

    private fun coinIcon(asset: AssetInfo) =
        when (asset) {
            CryptoCurrency.BTC -> R.drawable.vector_bitcoin_white
            CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_white
            else -> throw NotImplementedError("${asset.networkTicker} Not implemented")
        }

    /**
     * Pass null as the parameter here to close the view without triggering any [AssetInfo]
     * change listeners.
     */
    private fun closeLayout(cryptoCurrency: AssetInfo?) {
        // Update UI
        cryptoCurrency?.run { setCurrentlySelectedCurrency(this) }
        // Trigger layout change
        animateLayout(false)
        // Fade in title
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }
        binding.textviewSelectedCurrency.startAnimation(alphaAnimation)
        alphaAnimation.setAnimationListener {
            onAnimationEnd {
                binding.textviewSelectedCurrency.alpha = 1.0f
                // Inform parent of currency selection once animation complete to avoid glitches
                cryptoCurrency?.run { selectionListener(this) }
            }
        }
    }

    fun getSelectedCurrency() = selectedCurrency

    private inner class CustomOutline constructor(
        var width: Int,
        var height: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRect(0, 0, width, height)
        }
    }

    private inner class ExpandAnimation(
        private val startHeight: Int,
        endHeight: Int
    ) :
        Animation() {

        private val deltaHeight: Int = endHeight - startHeight

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val params = binding.contentFrame.layoutParams
            params.height = (startHeight + deltaHeight * interpolatedTime).toInt()
            binding.contentFrame.layoutParams = params
        }

        override fun willChangeBounds(): Boolean = true
    }

    fun isTouchOutside(event: MotionEvent): Boolean {
        val viewRect = Rect()
        getGlobalVisibleRect(viewRect)
        return !viewRect.contains(event.rawX.toInt(), event.rawY.toInt())
    }
}
