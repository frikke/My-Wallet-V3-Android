package piuk.blockchain.android.ui.transfer.receive.detail

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.graphics.RectangleShape
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.analytics.events.RequestAnalyticsEvents
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.presentation.koin.scopedInject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityReceiveDetailsBinding
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.transfer.receive.plugin.ReceiveMemoView
import piuk.blockchain.android.util.copyToClipboardWithConfirmationDialog
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.putAccount

class ReceiveDetailActivity :
    MviActivity<ReceiveDetailModel, ReceiveDetailIntent, ReceiveDetailState, ActivityReceiveDetailsBinding>(),
    SlidingModalBottomDialog.Host {
    override val model: ReceiveDetailModel by scopedInject()
    private val encoder: QRCodeEncoder by inject()

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
        setContentView(binding.root)

        updateToolbar(
            toolbarTitle = getString(R.string.tx_title_receive, account?.currency?.displayTicker),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        account?.let {
            model.process(InitWithAccount(it))
            binding.receiveAccountDetails.updateItem(AccountListViewItem.Crypto(it))
        } ?: finish()

        with(binding) {
            shareButton.apply {
                text = getString(R.string.receive_share)
                buttonState = ButtonState.Disabled
            }
            copyButton.isEnabled = false
            qrImage.invisible()
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
            if (addressAvailable) {
                copyButton.setOnClickListener {
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
            } else {
                copyButton.setOnClickListener { }
            }
            copyButton.isEnabled = addressAvailable

            shareButton.apply {
                isEnabled = addressAvailable
                onClick = {
                    if (addressAvailable) {
                        shareAddress()
                    }
                }
                buttonState = if (addressAvailable) ButtonState.Enabled else ButtonState.Disabled
            }

            qrImage.visibleIf { addressAvailable }

            if (newState.qrUri != null) {
                qrBitmap = encoder.encodeAsBitmap(newState.qrUri, DIMENSION_QR_CODE)
                qrBitmap?.let { qrBitmap ->
                    qrImage.apply {
                        image = ImageResource.LocalWithResolvedBitmap(
                            bitmap = qrBitmap,
                            contentDescription = getString(R.string.scan_qr),
                            shape = RectangleShape
                        )
                    }
                }
            }

            receivingAddress.apply {
                text = newState.cryptoAddress.address
                setTextIsSelectable(true)
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

            if (newState.shouldShowRotatingAddressInfo()) {
                walletAddressLabel.apply {
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_question, 0)
                    setOnClickListener { showAddressInfoView() }
                }
            }

            setCustomSlot(newState)
        }
    }

    private fun openShareList() {
        account?.let {
            showBottomSheet(ShareAddressBottomSheet.newInstance(it))
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

    private fun shareAddress() {
        run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ -> openShareList() }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
        analytics.logEvent(RequestAnalyticsEvents.RequestPaymentClicked)
    }

    companion object {
        private const val PARAM_ACCOUNT = "account_param"
        const val DIMENSION_QR_CODE = 600

        fun newIntent(context: Context, account: CryptoAccount): Intent =
            Intent(context, ReceiveDetailActivity::class.java).apply {
                putAccount(PARAM_ACCOUNT, account)
            }
    }

    override fun onSheetClosed() {
        // do nothing
    }
}
