package piuk.blockchain.android.ui.transactionflow.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.bumptech.glide.Glide
import java.lang.IllegalStateException
import piuk.blockchain.android.databinding.TxFlowInfoBottomSheetLayoutBinding
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowBottomSheetInfo
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class TransactionFlowInfoBottomSheet : SlidingModalBottomDialog<TxFlowInfoBottomSheetLayoutBinding>() {

    private val info: TransactionFlowBottomSheetInfo by lazy {
        arguments?.getParcelable(INFO) as? TransactionFlowBottomSheetInfo ?: throw IllegalStateException(
            "Missing Required Info"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): TxFlowInfoBottomSheetLayoutBinding =
        TxFlowInfoBottomSheetLayoutBinding.inflate(inflater, container, false)

    override fun initControls(binding: TxFlowInfoBottomSheetLayoutBinding) {
        with(binding) {
            title.text = info.title
            description.text = info.description

            info.action?.let {
                infoActionContainer.visible()
                actionCta.text = it.ctaActionText
                actionDescription.text = it.description
                actionTitle.text = it.title
                actionCta.setOnClickListener {
                    (host as? TransactionFlowInfoHost)?.onActionInfoTriggered()
                }
                Glide.with(requireActivity()).load(it.icon).into(actionIcon)
            } ?: kotlin.run {
                infoActionContainer.gone()
            }
        }
    }

    companion object {
        private const val INFO = "INFO"
        fun newInstance(info: TransactionFlowBottomSheetInfo): TransactionFlowInfoBottomSheet =
            TransactionFlowInfoBottomSheet().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(INFO, info)
                    }
            }
    }
}

interface TransactionFlowInfoHost : SlidingModalBottomDialog.Host {
    fun onActionInfoTriggered()
}
