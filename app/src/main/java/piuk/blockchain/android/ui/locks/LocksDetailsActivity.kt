package piuk.blockchain.android.ui.locks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.utils.unsafeLazy
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS

class LocksDetailsActivity : BlockchainActivity() {

    private val fundsLocks: FundsLocks by unsafeLazy {
        intent?.getSerializableExtra(KEY_LOCKS) as FundsLocks
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocksDetailsScreen(
                locks = fundsLocks,
                backClicked = { onBackPressedDispatcher.onBackPressed() },
                learnMoreClicked = {
                    openUrl(TRADING_ACCOUNT_LOCKS)
                },
                contactSupportClicked = {
                    startActivity(SupportCentreActivity.newIntent(this, FUND_LOCKS_SUPPORT))
                },
                okClicked = { finish() }
            )
        }
    }

    companion object {
        private const val KEY_LOCKS = "LOCKS"
        private const val FUND_LOCKS_SUPPORT = "Funds Locks"
        fun start(
            context: Context,
            fundsLocks: FundsLocks
        ) = context.startActivity(
            Intent(context, LocksDetailsActivity::class.java).apply {
                putExtra(KEY_LOCKS, fundsLocks)
            }
        )
    }
}
