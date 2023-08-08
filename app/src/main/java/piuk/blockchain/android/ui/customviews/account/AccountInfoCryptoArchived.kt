package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewAccountCryptoOverviewArchivedBinding
import piuk.blockchain.android.ui.resources.AssetResources

class AccountInfoCryptoArchived @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val binding: ViewAccountCryptoOverviewArchivedBinding = ViewAccountCryptoOverviewArchivedBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val assetResources: AssetResources by inject()

    fun updateAccount(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) = updateView(account, onAccountClicked)

    private fun updateView(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        val asset = account.currency
        with(binding) {
            walletName.text = account.label
            assetResources.loadAssetIcon(icon, asset)
            icon.visible()

            if (account is TradingAccount) {
                assetAccountIcon.setImageResource(R.drawable.ic_custodial_account_indicator)
                assetAccountIcon.visible()
            } else {
                assetAccountIcon.gone()
            }
            container.alpha = .6f
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
