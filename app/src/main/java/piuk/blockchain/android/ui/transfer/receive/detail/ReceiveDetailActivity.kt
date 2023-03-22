package piuk.blockchain.android.ui.transfer.receive.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.icons.Copy
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.presentation.copyToClipboardWithConfirmationDialog
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.abbreviate
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.isLayer2Token
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityReceiveDetailsBinding
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.transfer.receive.plugin.ReceiveMemoView

class ReceiveDetailActivity :
    MviActivity<ReceiveDetailModel, ReceiveDetailIntent, ReceiveDetailState, ActivityReceiveDetailsBinding>(),
    SlidingModalBottomDialog.Host {
    override val model: ReceiveDetailModel by scopedInject()
    private val encoder: QRCodeEncoder by inject()
    private val assetCatalogue: AssetCatalogue by scopedInject()

    private var qrBitmap: Bitmap? = null

    override val alwaysDisableScreenshots: Boolean
        get() = false

    val account: CryptoAccount?
        get() = intent?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun initBinding(): ActivityReceiveDetailsBinding = ActivityReceiveDetailsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateToolbar(
            toolbarTitle = getString(R.string.tx_title_receive, account?.currency?.displayTicker),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        updateToolbarBackground(mutedBackground = true)

        account?.run {
            // header icon
            val l2Network = (account as? CryptoNonCustodialAccount)?.currency
                ?.takeIf { it.isLayer2Token }
                ?.coinNetwork

            val mainIcon = ImageResource.Remote(currency.logo)
            var tagUrl: String? = null
            val tagIcon = l2Network?.nativeAssetTicker
                ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }
                ?.also { tagUrl = it }
                ?.let { ImageResource.Remote(it) }
            val icon = tagIcon?.let {
                StackedIcon.SmallTag(
                    main = mainIcon,
                    tag = tagIcon
                )
            } ?: StackedIcon.SingleIcon(mainIcon)

            updateToolbarIcon(icon)

            // data
            model.process(InitWithAccount(this))

            with(binding) {
                // network
                l2Network?.let {
                    network.apply {
                        visible()
                        iconUrl = tagUrl
                        text = getString(R.string.coinview_asset_l1, currency.displayTicker, l2Network.name)
                    }
                }

                copyButton.apply {
                    text = getString(R.string.receive_copy)
                    textColor = Grey900
                    this@apply.icon = Icons.Copy.withTint(textColor).withSize(24.dp)
                    buttonState = ButtonState.Disabled
                }
                qrImage.invisible()
            }
        }
    }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun render(newState: ReceiveDetailState) {
        with(binding) {
            updateToolbar(
                toolbarTitle = getString(R.string.tx_title_receive, newState.account.currency.displayTicker),
                backAction = { onBackPressedDispatcher.onBackPressed() }
            )

            val addressAvailable = newState.qrUri != null

            copyButton.apply {
                isEnabled = addressAvailable
                onClick = {
                    if (addressAvailable) {
                        analytics.logEvent(
                            TransferAnalyticsEvent.ReceiveDetailsCopied(
                                accountType = TxFlowAnalyticsAccountType.fromAccount(newState.account),
                                asset = account?.currency ?: throw IllegalStateException(
                                    "Account asset is missing"
                                )
                            )
                        )

                        copyToClipboardWithConfirmationDialog(
                            confirmationAnchorView = binding.root,
                            confirmationMessage = R.string.receive_address_to_clipboard,
                            label = getString(R.string.send_address_title),
                            text = newState.cryptoAddress.address
                        )
                    }
                }
                buttonState = if (addressAvailable) ButtonState.Enabled else ButtonState.Disabled
            }

            qrImage.visibleIf { addressAvailable }

            if (newState.qrUri != null) {
                qrBitmap = encoder.encodeAsBitmap(newState.qrUri, DIMENSION_QR_CODE)
                qrBitmap?.let { qrBitmap ->
                    qrImage.apply {
                        squared = true
                        image = ImageResource.LocalWithResolvedBitmap(
                            bitmap = qrBitmap,
                            contentDescription = getString(R.string.scan_qr),
                            shape = RectangleShape
                        )
                    }
                }
            }

            receivingAddress.apply {
                text = newState.cryptoAddress.address.abbreviate(
                    startLength = ADDRESS_ABBREVIATION_LENGTH,
                    endLength = ADDRESS_ABBREVIATION_LENGTH
                )
                setTextIsSelectable(true)

                if (newState.shouldShowRotatingAddressInfo()) {
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_question, 0)
                    setOnClickListener { showAddressInfoView() }
                }
            }

            newState.networkName?.let { networkName ->
                networkAlert.apply {
                    text = getString(
                        R.string.receive_network_warning,
                        account?.currency?.displayTicker,
                        networkName
                    )
                    setOnClickListener { showMultichainInfoView(networkName) }
                    visible()
                }
            }

            setCustomSlot(newState)
        }
    }

    private fun showAddressInfoView() {
        account?.let {
            showBottomSheet(
                AddressInfoReceiveBottomSheet.newInstance(
                    displayTicker = it.currency.displayTicker,
                    label = it.label
                )
            )
        }
    }

    private fun showMultichainInfoView(networkName: String) {
        account?.let {
            showBottomSheet(MultichainInfoBottomSheet.newInstance(it, networkName))
        }
    }

    private fun setCustomSlot(newState: ReceiveDetailState) {
        when {
            newState.shouldShowXlmMemo() -> ReceiveMemoView(this).also {
                it.updateAddress(newState.cryptoAddress)
            }
            // TODO: SEGWIT LEGACY SELECTOR
            else -> null
        }?.let {
            // only add view once if it doesn't exist
            if (it.shouldAddMemoView()) {
                binding.customisationSlots.addView(it)
            }
        }
    }

    private fun ConstraintLayout.shouldAddMemoView() =
        this is ReceiveMemoView && binding.customisationSlots.findViewById<ConstraintLayout>(
            R.id.receive_memo_parent
        ) == null

    companion object {
        private const val PARAM_ACCOUNT = "account_param"
        const val DIMENSION_QR_CODE = 800
        private const val ADDRESS_ABBREVIATION_LENGTH = 6
        fun newIntent(context: Context, account: CryptoAccount): Intent =
            Intent(context, ReceiveDetailActivity::class.java).apply {
                putAccount(PARAM_ACCOUNT, account)
            }
    }

    override fun onSheetClosed() {
        // do nothing
    }
}
