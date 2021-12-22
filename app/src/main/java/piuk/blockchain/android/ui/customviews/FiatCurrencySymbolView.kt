package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import info.blockchain.balance.FiatCurrency
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.ui.resources.AssetResources

class FiatCurrencySymbolView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), KoinComponent {

    private val assetResources: AssetResources by inject()

    fun setIcon(fiat: FiatCurrency) =
        assetResources.loadAssetIcon(this, fiat)
}
