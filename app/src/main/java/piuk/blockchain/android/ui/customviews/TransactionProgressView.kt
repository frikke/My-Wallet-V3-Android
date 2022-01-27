package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import info.blockchain.balance.Currency
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewTransactionProgressBinding
import piuk.blockchain.android.ui.resources.AssetResources

class TransactionProgressView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs), KoinComponent {

    private val assetResources: AssetResources by inject()

    private val binding: ViewTransactionProgressBinding =
        ViewTransactionProgressBinding.inflate(LayoutInflater.from(context), this, true)

    fun setAssetIcon(@DrawableRes assetIcon: Int) {
        binding.txIcon.setImageResource(assetIcon)
    }

    fun setAssetIcon(asset: Currency) {
        assetResources.loadAssetIcon(binding.txIcon, asset)
    }

    fun onCtaClick(text: String, fn: () -> Unit) {
        binding.txOkBtn.apply {
            visible()
            this.text = text
            setOnClickListener { fn() }
        }
    }

    fun onSecondaryCtaClicked(text: String, fn: () -> Unit) {
        binding.secondaryBtn.apply {
            visible()
            this.text = text
            setOnClickListener { fn() }
        }
    }

    fun showTxInProgress(title: String, subtitle: String) {
        with(binding) {
            progress.visible()
            txStateIndicator.gone()
            txOkBtn.gone()
        }
        setText(title, subtitle)
    }

    fun showTxPending(title: String, subtitle: String) {
        with(binding) {
            progress.gone()
            txStateIndicator.visible()
            txOkBtn.visible()
            txStateIndicator.setImageResource(R.drawable.ic_pending_clock)
        }
        setText(title, subtitle)
    }

    fun showTxSuccess(
        title: String,
        subtitle: String,
        icon: Int = R.drawable.ic_check_circle
    ) {
        with(binding) {
            txStateIndicator.setImageResource(icon)
            txStateIndicator.visible()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showPendingTx(
        title: String,
        subtitle: String,
        locksNote: SpannableStringBuilder
    ) {
        with(binding) {
            txStateIndicator.setImageResource(R.drawable.ic_check_circle)
            txStateIndicator.visible()
            showEndStateUi()
            txTitle.text = title
            txSubtitle.text = subtitle
            txNoteLocks.run {
                setText(locksNote, TextView.BufferType.SPANNABLE)
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun showTxError(
        title: String,
        subtitle: CharSequence,
        resourceIcon: Int = R.drawable.ic_alert_white_bkgd
    ) {
        with(binding) {
            txStateIndicator.setImageResource(resourceIcon)
            txStateIndicator.visible()
            progress.gone()
        }
        setText(title, subtitle)
    }

    fun showFiatTxSuccess(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        with(binding.txStateIndicator) {
            setImageResource(R.drawable.ic_tx_deposit_w_green_bkgd)
            visible()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showFiatTxPending(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        showTxInProgress(title, subtitle)
    }

    fun showFiatTxError(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        with(binding) {
            txIcon.setImageResource(R.drawable.ic_alert_logo)
            txStateIndicator.gone()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    private fun setFiatAssetIcon(currency: String) =
        setAssetIcon(
            when (currency) {
                "EUR" -> R.drawable.ic_funds_euro_masked
                "GBP" -> R.drawable.ic_funds_euro_masked
                else -> R.drawable.ic_funds_usd_masked
            }
        )

    private fun showEndStateUi() {
        with(binding) {
            progress.gone()
            txOkBtn.visible()
        }
    }

    private fun setText(title: String, subtitle: CharSequence) {
        with(binding) {
            txTitle.text = title
            txSubtitle.apply {
                text = subtitle
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }
}
