package piuk.blockchain.android.cards.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import com.braintreepayments.cardform.view.CvvEditText
import piuk.blockchain.android.R

class CardCvvEditText : CvvEditText {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var errorMessage: String = resources.getString(com.blockchain.stringResources.R.string.invalid_cvv)

    fun setErrorMessage(@StringRes stringRes: Int) {
        errorMessage = resources.getString(stringRes)
    }

    override fun getErrorMessage(): String {
        return errorMessage
    }
}
