package piuk.blockchain.android.ui.locks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.blockchain.core.payments.model.FundsLocks
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityOnHoldDetailsBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setUpToolbar()
        setUpRecyclerView()
        setUpTextInfo()
    }

    private fun setUpToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.funds_locked_details_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { this.finish() }
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
        }
    }

    companion object {
        private const val KEY_LOCKS = "LOCKS"
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
