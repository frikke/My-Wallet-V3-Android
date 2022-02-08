package piuk.blockchain.android.ui.locks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.core.payments.model.FundsLocks
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityOnHoldDetailsBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class LocksDetailsActivity : BlockchainActivity() {

    private val binding: ActivityOnHoldDetailsBinding by lazy {
        ActivityOnHoldDetailsBinding.inflate(layoutInflater)
    }

    private val fundsLocks: FundsLocks by unsafeLazy {
        intent?.getSerializableExtra(KEY_LOCKS) as FundsLocks
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.funds_locked_details_toolbar),
            backAction = { onBackPressed() }
        )
        setUpRecyclerView()
        setUpTextInfo()
    }

    private fun setUpRecyclerView() {
        val locksDetailsDelegateAdapter = LocksDetailsDelegateAdapter()
        locksDetailsDelegateAdapter.items = fundsLocks.locks
        binding.recyclerViewLocks.apply {
            adapter = locksDetailsDelegateAdapter
            addItemDecoration(BlockchainListDividerDecor(context))
        }
    }

    private fun setUpTextInfo() {
        val amountOnHold = fundsLocks.onHoldTotalAmount.toStringWithSymbol()
        with(binding) {
            totalAmount.text = amountOnHold
            titleAmount.text = getString(R.string.funds_locked_details_title, amountOnHold)
            learnMore.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TRADING_ACCOUNT_LOCKS))) }

            contactSupport.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = setContactSupportLink(R.string.funds_locked_details_contact_support)
            }
        }
    }

    private fun setContactSupportLink(stringId: Int): CharSequence {
        return StringUtils.getStringWithMappedAnnotations(
            this,
            stringId,
            emptyMap()
        ) { onSupportClicked() }
    }

    private fun onSupportClicked() {
        startActivity(SupportCentreActivity.newIntent(this, FUND_LOCKS_SUPPORT))
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
