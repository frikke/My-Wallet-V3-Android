package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.api.ServerErrorAction
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.koin.scopedInject
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewTransactionProgressBinding
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.loadRemoteErrorAndStatusIcons
import piuk.blockchain.android.util.loadRemoteErrorIcon
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class TransactionProgressView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs), KoinComponent {

    interface TransactionProgressActions {
        fun onPrimaryButtonClicked()
        fun onSecondaryButtonClicked()
        fun onTertiaryButtonClicked()
    }

    private val assetResources: AssetResources by inject()
    private val compositeDisposable = CompositeDisposable()
    private val deeplinkRedirector: DeeplinkRedirector by scopedInject()

    private val binding: ViewTransactionProgressBinding =
        ViewTransactionProgressBinding.inflate(LayoutInflater.from(context), this, true)

    fun setAssetIcon(@DrawableRes assetIcon: Int) {
        binding.txIcon.setImageResource(assetIcon)
    }

    fun setAssetIcon(asset: Currency) {
        assetResources.loadAssetIcon(binding.txIcon, asset)
    }

    fun setupPrimaryCta(text: String, onClick: () -> Unit) {
        binding.txPrimaryBtn.apply {
            visible()
            this.text = text
            this.onClick = { onClick() }
        }
    }

    fun setupSecondaryCta(text: String, onClick: () -> Unit) {
        binding.txSecondaryBtn.apply {
            visible()
            this.text = text
            this.onClick = { onClick() }
        }
    }

    fun setupTertiaryCta(text: String, onClick: () -> Unit) {
        binding.txTertiaryBtn.apply {
            visible()
            this.text = text
            this.onClick = { onClick() }
        }
    }

    fun showTxInProgress(title: String, subtitle: String) {
        with(binding) {
            progress.visible()
            txStateIndicator.gone()
            txPrimaryBtn.gone()
        }
        setText(title, subtitle)
    }

    fun showTxPending(title: String, subtitle: String) {
        with(binding) {
            progress.gone()
            txStateIndicator.visible()
            txPrimaryBtn.visible()
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
        @DrawableRes statusIcon: Int = R.drawable.ic_alert_white_bkgd
    ) {
        with(binding) {
            txStateIndicator.setImageResource(statusIcon)
            txStateIndicator.visible()
            progress.gone()
        }
        setText(title, subtitle)
    }

    fun showServerSideError(
        iconUrl: String,
        statusIconUrl: String = "", // not all server side errors will have a status icon
        title: String,
        description: String,
        @DrawableRes defaultErrorStatusIcon: Int = R.drawable.ic_alert_white_bkgd
    ) {
        when {
            // we have been provided both icon and status
            iconUrl.isNotEmpty() && statusIconUrl.isNotEmpty() -> {
                loadRemoteErrorAndStatusIcons(
                    iconUrl,
                    statusIconUrl,
                    title,
                    description,
                    defaultErrorStatusIcon
                )
            }
            // we only have one icon
            iconUrl.isNotEmpty() && statusIconUrl.isEmpty() -> {
                loadRemoteErrorIcon(
                    iconUrl,
                    title,
                    description,
                    defaultErrorStatusIcon
                )
            }
            // no icons provided
            else -> showTxError(
                title = title,
                subtitle = description,
                statusIcon = defaultErrorStatusIcon
            )
        }
    }

    private fun loadRemoteErrorAndStatusIcons(
        iconUrl: String,
        statusIconUrl: String,
        title: String,
        description: String,
        @DrawableRes defaultStatusIcon: Int
    ) {
        context.loadRemoteErrorAndStatusIcons(
            iconUrl,
            statusIconUrl,
            onIconLoadSuccess = { drawable ->
                updateErrorIcon(
                    title = title,
                    subtitle = description,
                    icon = drawable
                )
            },
            onIconLoadError = {
                showTxError(
                    title = title,
                    subtitle = description,
                    statusIcon = defaultStatusIcon
                )
            },
            onStatusIconLoadSuccess = { drawable ->
                updateStatusIcon(
                    title = title,
                    subtitle = description,
                    statusIcon = drawable
                )
            },
            onStatusIconLoadError = {
                showTxError(
                    title = title,
                    subtitle = description,
                    statusIcon = defaultStatusIcon
                )
            }
        )
    }

    private fun loadRemoteErrorIcon(
        iconUrl: String,
        title: String,
        description: String,
        @DrawableRes defaultStatusIcon: Int
    ) {
        context.loadRemoteErrorIcon(
            iconUrl,
            onIconLoadSuccess = { drawable ->
                updateErrorIcon(
                    title = title,
                    subtitle = description,
                    icon = drawable
                )
            },
            onIconLoadError = {
                showTxError(
                    title = title,
                    subtitle = description,
                    statusIcon = defaultStatusIcon
                )
            }
        )
    }

    private fun updateStatusIcon(
        title: String,
        subtitle: CharSequence,
        statusIcon: Drawable
    ) {
        with(binding) {
            txStateIndicator.setImageDrawable(statusIcon)
            txStateIndicator.visible()
            progress.gone()
        }
        setText(title, subtitle)
    }

    private fun updateErrorIcon(
        title: String,
        subtitle: CharSequence,
        icon: Drawable
    ) {
        with(binding) {
            txIcon.setImageDrawable(icon)
            txStateIndicator.gone()
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

    fun showServerSideActionErrorCtas(
        list: List<ServerErrorAction>,
        currencyCode: String,
        onActionsClickedCallback: TransactionProgressActions
    ) {
        list.mapIndexed { index, item ->
            when (index) {
                0 -> {
                    setupPrimaryCta(
                        text = item.title
                    ) {
                        redirectToDeeplinkProcessor(item.deeplinkPath, currencyCode)
                        onActionsClickedCallback.onPrimaryButtonClicked()
                    }
                }
                1 -> {
                    setupSecondaryCta(
                        text = item.title
                    ) {
                        redirectToDeeplinkProcessor(item.deeplinkPath, currencyCode)
                        onActionsClickedCallback.onSecondaryButtonClicked()
                    }
                }
                2 -> {
                    setupTertiaryCta(
                        text = item.title
                    ) {
                        redirectToDeeplinkProcessor(item.deeplinkPath, currencyCode)
                        onActionsClickedCallback.onTertiaryButtonClicked()
                    }
                }
                else -> {
                    // do nothing - we only support 3 actions
                }
            }
        }
    }

    private fun redirectToDeeplinkProcessor(link: String, currencyCode: String) {
        compositeDisposable += deeplinkRedirector.processDeeplinkURL(
            link.appendTickerToDeeplink(currencyCode)
        ).emptySubscribe()
    }

    private fun String.appendTickerToDeeplink(currencyCode: String): Uri =
        Uri.parse("$this?code=$currencyCode")

    fun clearSubscriptions() {
        compositeDisposable.clear()
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
            txPrimaryBtn.visible()
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
