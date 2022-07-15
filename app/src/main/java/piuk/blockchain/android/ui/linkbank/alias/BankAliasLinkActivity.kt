package piuk.blockchain.android.ui.linkbank.alias

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.FragmentActivityBinding
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.extensions.exhaustive
import com.google.android.material.snackbar.Snackbar
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class BankAliasLinkActivity :
    BlockchainActivity(),
    NavigationRouter<BankAliasNavigationEvent> {

    private val currency: String
        get() = intent.getStringExtra(CURRENCY).orEmpty()

    private val binding: FragmentActivityBinding by lazy {
        FragmentActivityBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbar(
            toolbarTitle = getString(R.string.withdraw_to),
            backAction = { onSupportNavigateUp() }
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, BankAliasLinkFragment.newInstance(currency))
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun route(navigationEvent: BankAliasNavigationEvent) {
        when (navigationEvent) {
            is BankAliasNavigationEvent.BankAccountLinkedWithAlias -> {
                setResult(RESULT_OK, Intent().putExtra(ALIAS_LINK_SUCCESS, true))
                finish()
            }
            is BankAliasNavigationEvent.UnhandledError -> {
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.common_error), Snackbar.LENGTH_SHORT, SnackbarType.Error
                ).show()
            }
        }.exhaustive
    }

    companion object {
        const val ALIAS_LINK_SUCCESS = "ALIAS_LINK_SUCCESS"
        private const val CURRENCY = "CURRENCY"

        fun newInstance(currency: String, context: Context): Intent {
            val intent = Intent(context, BankAliasLinkActivity::class.java)
            intent.putExtra(CURRENCY, currency)
            return intent
        }
    }
}

class BankAliasLinkContract : ActivityResultContract<String, Boolean>() {

    override fun createIntent(context: Context, input: String): Intent =
        BankAliasLinkActivity.newInstance(input, context)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        intent?.getBooleanExtra(BankAliasLinkActivity.ALIAS_LINK_SUCCESS, false) ?: false
}
