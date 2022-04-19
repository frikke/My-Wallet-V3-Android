package piuk.blockchain.android.ui.transfer.receive.detail

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.events.RequestAnalyticsEvents
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogReceiveBinding
import piuk.blockchain.android.databinding.ReceiveShareRowBinding
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.transfer.receive.plugin.ReceiveInfoView
import piuk.blockchain.android.ui.transfer.receive.plugin.ReceiveMemoView
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.putAccount

internal class ReceiveDetailSheet :
    MviBottomSheet<ReceiveDetailModel, ReceiveDetailIntent, ReceiveDetailState, DialogReceiveBinding>() {
    override val model: ReceiveDetailModel by scopedInject()
    private val encoder: QRCodeEncoder by inject()
    private val receiveIntentHelper: ReceiveDetailIntentHelper by inject()

    private var qrBitmap: Bitmap? = null

    val account: CryptoAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogReceiveBinding =
        DialogReceiveBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogReceiveBinding) {
        account?.let {
            model.process(InitWithAccount(it))
            binding.receiveAccountDetails.updateAccount(it)
        } ?: dismiss()

        with(binding) {
            shareButton.apply {
                text = getString(R.string.receive_share)
                buttonState = ButtonState.Disabled
            }
            copyButton.isEnabled = false
            progressbar.visible()
            qrImage.invisible()
            shareBack.setOnClickListener {
                setBottomSheetDraggable(true)
                model.process(ClearShareList)
            }
        }
    }

    override fun render(newState: ReceiveDetailState) {
        if (newState.displayMode == ReceiveScreenDisplayMode.SHARE) {
            renderShare(newState)
        } else {
            renderReceive(newState)
        }
    }

    private fun renderReceive(newState: ReceiveDetailState) {
        with(binding) {
            switcher.displayedChild = VIEW_RECEIVE
            receiveTitle.text = getString(R.string.tx_title_receive, newState.account.currency.displayTicker)
            val addressAvailable = newState.qrUri != null
            if (addressAvailable) {
                shareButton.onClick = { shareAddress() }
                copyButton.setOnClickListener {
                    analytics.logEvent(
                        TransferAnalyticsEvent.ReceiveDetailsCopied(
                            accountType = TxFlowAnalyticsAccountType.fromAccount(newState.account),
                            asset = account?.currency ?: throw IllegalStateException(
                                "Account asset is missing"
                            )
                        )
                    )
                    copyAddress(newState.cryptoAddress.address)
                }
            } else {
                shareButton.onClick = {}
                copyButton.setOnClickListener { }
            }
            shareButton.buttonState = if (addressAvailable) ButtonState.Enabled else ButtonState.Disabled
            copyButton.isEnabled = addressAvailable

            progressbar.visibleIf { addressAvailable.not() }
            qrImage.visibleIf { addressAvailable }

            if (newState.qrUri != null && qrImage.drawable == null) {
                qrBitmap = encoder.encodeAsBitmap(newState.qrUri, DIMENSION_QR_CODE)
                qrImage.setImageBitmap(qrBitmap)
            }
            receivingAddress.apply {
                text = newState.cryptoAddress.address
                setTextIsSelectable(true)
            }
        }

        setCustomSlot(newState)
    }

    private fun renderShare(newState: ReceiveDetailState) {
        with(binding) {
            switcher.displayedChild = VIEW_SHARE
            check(newState.qrUri != null)
            val dataIntent = qrBitmap?.let {
                receiveIntentHelper.getIntentDataList(
                    uri = newState.qrUri, bitmap = it, asset = newState.account.currency
                )
            } ?: emptyList()

            shareTitle.text = getString(R.string.receive_share_title, newState.account.currency.displayTicker)
            setBottomSheetDraggable(false)
            with(shareList) {
                layoutManager = LinearLayoutManager(context)
                adapter = ShareListAdapter(dataIntent).apply {
                    itemClickedListener = { dismiss() }
                }
            }
        }
    }

    private fun setBottomSheetDraggable(isDraggable: Boolean) {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.root.parent as View)
        bottomSheetBehavior.isDraggable = isDraggable
    }

    private fun setCustomSlot(newState: ReceiveDetailState) {
        when {
            newState.shouldShowXlmMemo() -> ReceiveMemoView(requireContext()).also {
                it.updateAddress(newState.cryptoAddress)
            }
            newState.shouldShowRotatingAddressInfo() -> ReceiveInfoView(requireContext()).also {
                it.update(newState.account) {
                    binding.customisationSlots.findViewById<ConstraintLayout>(R.id.receive_info_parent)?.gone()
                }
            }
            // TODO: SEGWIT LEGACY SELECTOR
            else -> null
        }?.let {
            // only add view once if it doesn't exist
            if (it.shouldAddInfoView() || it.shouldAddMemoView()) {
                binding.customisationSlots.addView(it)
            }
        }
    }

    private fun ConstraintLayout.shouldAddInfoView() =
        this is ReceiveInfoView && binding.customisationSlots.findViewById<ConstraintLayout>(
            R.id.receive_info_parent
        ) == null

    private fun ConstraintLayout.shouldAddMemoView() =
        this is ReceiveMemoView && binding.customisationSlots.findViewById<ConstraintLayout>(
            R.id.receive_memo_parent
        ) == null

    private fun shareAddress() {
        activity.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ -> model.process(ShowShare) }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
        analytics.logEvent(RequestAnalyticsEvents.RequestPaymentClicked)
    }

    private fun copyAddress(address: String) {
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", address)
                    BlockchainSnackbar.make(
                        dialog?.window?.decorView ?: binding.root,
                        getString(R.string.copied_to_clipboard), type = SnackbarType.Success
                    ).show()
                    clipboard.setPrimaryClip(clip)
                }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
    }

    companion object {
        private const val PARAM_ACCOUNT = "account_param"
        private const val DIMENSION_QR_CODE = 600

        private const val VIEW_RECEIVE = 0
        private const val VIEW_SHARE = 1

        fun newInstance(account: CryptoAccount): ReceiveDetailSheet =
            ReceiveDetailSheet().apply {
                arguments = Bundle().apply {
                    putAccount(PARAM_ACCOUNT, account)
                }
            }
    }
}

private class ShareListAdapter(private val paymentCodeData: List<SendPaymentCodeData>) :
    RecyclerView.Adapter<ShareListAdapter.ViewHolder>() {

    var itemClickedListener: () -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row = ReceiveShareRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(row)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = paymentCodeData[position]
        holder.bind(data) {
            itemClickedListener()
        }
    }

    override fun getItemCount() = paymentCodeData.size

    class ViewHolder(private val binding: ReceiveShareRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: SendPaymentCodeData, onClick: () -> Unit) {
            binding.shareAppTitle.text = data.title
            binding.shareAppImage.setImageDrawable(data.logo)

            binding.root.setOnClickListener {
                onClick.invoke()
                try {
                    itemView.context.startActivity(data.intent)
                } catch (e: SecurityException) {
                    BlockchainSnackbar.make(
                        itemView,
                        itemView.context.getString(R.string.share_failed, data.title),
                        type = SnackbarType.Error
                    ).show()
                }
            }
        }
    }
}
