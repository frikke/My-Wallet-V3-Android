package piuk.blockchain.android.ui.transfer.receive.detail

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.presentation.koin.scopedInject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ReceiveShareRowBinding
import piuk.blockchain.android.databinding.ShareAddressBottomSheetBinding
import piuk.blockchain.android.scan.QRCodeEncoder
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.putAccount

internal class ShareAddressBottomSheet :
    MviBottomSheet<ReceiveDetailModel, ReceiveDetailIntent, ReceiveDetailState, ShareAddressBottomSheetBinding>() {
    override val model: ReceiveDetailModel by scopedInject()
    private val receiveIntentHelper: ReceiveDetailIntentHelper by inject()
    private val encoder: QRCodeEncoder by inject()

    private var qrBitmap: Bitmap? = null

    val account: CryptoAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ShareAddressBottomSheetBinding =
        ShareAddressBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: ShareAddressBottomSheetBinding) {
        account?.let {
            model.process(InitWithAccount(it))
        }
    }

    override fun render(newState: ReceiveDetailState) {
        if (newState.qrUri != null) {
            qrBitmap = encoder.encodeAsBitmap(newState.qrUri, ReceiveDetailActivity.DIMENSION_QR_CODE)
        }

        with(binding) {
            toolbar.navigationToolbar.title =
                getString(R.string.receive_share_title, newState.account.currency.displayTicker)
            toolbar.navigationToolbar.onBackButtonClick = { dismiss() }

            val dataIntent = newState.qrUri?.let {
                qrBitmap?.let {
                    receiveIntentHelper.getIntentDataList(
                        uri = newState.qrUri, bitmap = it, asset = newState.account.currency
                    )
                }
            } ?: emptyList()

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

    companion object {
        private const val PARAM_ACCOUNT = "PARAM_ACCOUNT"

        fun newInstance(
            account: CryptoAccount
        ): ShareAddressBottomSheet =
            ShareAddressBottomSheet().apply {
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
