package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewAccountGroupOverviewBinding
import piuk.blockchain.android.util.getResolvedDrawable
import timber.log.Timber

class AccountInfoGroup @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val binding: ViewAccountGroupOverviewBinding =
        ViewAccountGroupOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    private val exchangeRates: ExchangeRatesDataManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    private val disposables = CompositeDisposable()

    fun updateAccount(account: AccountGroup) {
        disposables.clear()
        updateView(account)
    }

    private fun updateView(account: AccountGroup) {
        // Only supports AllWallets at this time
        require(account is AllWalletsAccount)

        disposables.clear()

        val currency = currencyPrefs.selectedFiatCurrency
        with(binding) {
            icon.setImageDrawable(context.getResolvedDrawable(R.drawable.ic_all_wallets_blue))

            walletName.text = account.label

            assetSubtitle.text = context.getString(R.string.activity_wallet_total_balance)

            walletBalanceFiat.invisible()
            walletCurrency.text = currency.displayTicker

            disposables += account.balance.firstOrError()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        walletBalanceFiat.text = it.total.toStringWithSymbol()
                        walletBalanceFiat.visible()
                    },
                    onError = {
                        Timber.e("Cannot get balance for ${account.label}")
                    }
                )
        }
    }

    fun dispose() {
        disposables.clear()
    }
}
