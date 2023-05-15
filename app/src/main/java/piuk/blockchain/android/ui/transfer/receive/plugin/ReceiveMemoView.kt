package piuk.blockchain.android.ui.transfer.receive.plugin

import android.content.Context
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.CryptoAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewReceiveMemoBinding
import piuk.blockchain.android.urllinks.URL_XLM_MEMO
import piuk.blockchain.android.util.StringUtils

class ReceiveMemoView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle) {

    private val binding: ViewReceiveMemoBinding =
        ViewReceiveMemoBinding.inflate(LayoutInflater.from(context), this, true)

    fun updateAddress(cryptoAddress: CryptoAddress) {
        require(cryptoAddress.memo != null)

        with(binding) {
            val assetName = cryptoAddress.asset.displayTicker
            memoText.apply {
                text = cryptoAddress.memo
                setTextIsSelectable(true)
            }
            memoLabel.text = resources.getString(com.blockchain.stringResources.R.string.receive_memo_title, assetName)
            memoWarn.text = resources.getString(com.blockchain.stringResources.R.string.receive_memo_warning, assetName)

            val linkMap = mapOf<String, Uri>("learn_more_link" to Uri.parse(URL_XLM_MEMO))
            memoLink.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = StringUtils.getStringWithMappedAnnotations(
                    context,
                    com.blockchain.stringResources.R.string.common_linked_learn_more,
                    linkMap
                )
            }
        }
    }
}
