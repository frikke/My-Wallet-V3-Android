package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visibleIf
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCheckoutHeaderBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations

class SimpleInfoHeaderView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), ConfirmSheetWidget, KoinComponent {

    private lateinit var model: TransactionModel
    private lateinit var customiser: TransactionConfirmationCustomisations
    private lateinit var analytics: TxFlowAnalytics
    var shouldShowExchange = true

    private val binding: ViewCheckoutHeaderBinding =
        ViewCheckoutHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    override fun initControl(
        model: TransactionModel,
        customiser: TransactionConfirmationCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }
        with(binding) {
            state.pendingTx?.amount?.let { amount ->
                headerTitle.text = amount.toStringWithSymbol()
                val previousFiatAmount = headerSubtitle.text
                var newFiatAmount = ""
                if (shouldShowExchange) {
                    state.confirmationRate?.let {
                        newFiatAmount = it.convert(amount, false).toStringWithSymbol()
                        headerSubtitle.text = newFiatAmount
                    }
                } else {
                    headerSubtitle.gone()
                }
                if (previousFiatAmount.isNotEmpty() && previousFiatAmount != newFiatAmount) {
                    headerTitle.animateChange {
                        headerTitle.setTextColor(
                            ContextCompat.getColor(headerTitle.context, R.color.grey_800)
                        )
                    }
                    headerSubtitle.animateChange {
                        headerSubtitle.setTextColor(
                            ContextCompat.getColor(headerSubtitle.context, R.color.grey_600)
                        )
                    }
                }
            }
        }
    }

    // TODO we will use the extension created in the buy spike and remove it from here and from swapInfoHeaderView
    private fun TextView.animateChange(onAnimationEnd: () -> Unit) {
        pivotX = this.measuredWidth * 0.5f
        pivotY = this.measuredHeight * 0.5f

        setTextColor(ContextCompat.getColor(context, R.color.blue_600))

        animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                this.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        onAnimationEnd()
                    }
            }
    }

    fun setDetails(title: String, subtitle: String) {
        with(binding) {
            headerTitle.goneIf { title.isBlank() }
            headerSubtitle.goneIf { subtitle.isBlank() }

            headerTitle.text = title
            headerSubtitle.text = subtitle
        }
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }
}

class EmptyHeaderView : ConfirmSheetWidget {
    override fun initControl(
        model: TransactionModel,
        customiser: TransactionConfirmationCustomisations,
        analytics: TxFlowAnalytics
    ) {
    }

    override fun update(state: TransactionState) {}

    override fun setVisible(isVisible: Boolean) {}
}
