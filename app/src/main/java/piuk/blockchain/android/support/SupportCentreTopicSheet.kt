package piuk.blockchain.android.support

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.button.ButtonState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetSupportTopicBinding

class SupportCentreTopicSheet : SlidingModalBottomDialog<BottomSheetSupportTopicBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun onTopicSelected(topic: String)
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a SupportCentreTopicSheet.Host")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSupportTopicBinding =
        BottomSheetSupportTopicBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetSupportTopicBinding) {
        with(binding) {
            zendeskOptions.setOnCheckedChangeListener { _, _ ->
                zendeskContinue.buttonState = ButtonState.Enabled
            }

            zendeskContinue.apply {
                text = getString(com.blockchain.stringResources.R.string.common_continue)
                buttonState = ButtonState.Disabled
                onClick = {
                    val checkedButton = zendeskOptions.findViewById<RadioButton>(zendeskOptions.checkedRadioButtonId)
                    host.onTopicSelected(checkedButton.text.toString())
                    dismiss()
                }
            }
        }
    }

    companion object {
        fun newInstance() = SupportCentreTopicSheet()
    }
}
