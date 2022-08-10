package piuk.blockchain.android.cards.views

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import androidx.core.widget.TextViewCompat
import com.blockchain.preferences.SimpleBuyPrefs
import com.braintreepayments.cardform.view.CardEditText
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class CardNumberEditText : CardEditText, KoinComponent {
    interface CardNumberListener {
        fun onPaste()
        fun onCut()
        fun onCopy()
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private lateinit var listener: CardNumberListener

    private val supportedCardTypes: String by unsafeLazy {
        simpleBuyPrefs.getSupportedCardTypes() ?: "VISA"
    }

    override fun getErrorMessage(): String {
        return if (cardNumberIsInvalid())
            resources.getString(R.string.invalid_card_number)
        else
            resources.getString(R.string.card_not_supported)
    }

    private fun cardNumberIsInvalid(): Boolean = super.isValid().not()

    fun attachListener(listener: CardNumberListener) {
        this.listener = listener
    }

    override fun afterTextChanged(editable: Editable) {
        super.afterTextChanged(editable)
        if (supportedCardTypes.contains(cardType.name)) {
            updateIcon(cardType.icon())
        } else updateIcon(0)
    }

    private fun updateIcon(frontResource: Int) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
            this,
            0,
            0,
            frontResource,
            0
        )
    }

    override fun isValid(): Boolean {
        return super.isValid() && supportedCardTypes.contains(cardType.name)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        val superAction = super.onTextContextMenuItem(id)
        if (::listener.isInitialized) {
            when (id) {
                android.R.id.cut -> listener.onCut()
                android.R.id.copy -> listener.onCopy()
                android.R.id.paste -> listener.onPaste()
            }
        }
        return superAction
    }
}
