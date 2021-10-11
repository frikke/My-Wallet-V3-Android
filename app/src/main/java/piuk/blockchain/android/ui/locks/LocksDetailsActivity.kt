package piuk.blockchain.android.ui.locks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.widget.Toolbar
import com.blockchain.core.payments.model.Withdrawals
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityOnHoldDetailsBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class LocksDetailsActivity : BlockchainActivity() {

    private val binding: ActivityOnHoldDetailsBinding by lazy {
        ActivityOnHoldDetailsBinding.inflate(layoutInflater)
    }

    private val withdrawals: Withdrawals by unsafeLazy {
        intent?.getSerializableExtra(LOCK) as Withdrawals
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
        setupToolbar(toolbar, R.string.withdrawal_details_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { this.finish() }
    }

    private fun setUpRecyclerView() {
        val locksDetailsDelegateAdapter = LocksDetailsDelegateAdapter()
        locksDetailsDelegateAdapter.items = withdrawals.locks
        binding.recyclerViewLocks.apply {
            adapter = locksDetailsDelegateAdapter
            addItemDecoration(BlockchainListDividerDecor(context))
        }
    }

    private fun setUpTextInfo() {
        with(binding) {
            text.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = setLearnMoreLink(R.string.withdrawal_details_text)
            }
            totalAmount.text = withdrawals.onHoldTotalAmount.toStringWithSymbol()
        }
    }

    private fun setLearnMoreLink(stringId: Int): CharSequence {
        val linksMap = mapOf<String, Uri>(
            "learn_more" to Uri.parse(TRADING_ACCOUNT_LOCKS)
        )
        return StringUtils.getStringWithMappedAnnotations(
            this,
            stringId,
            linksMap
        )
    }

    companion object {
        private const val LOCK = "LOCK"
        fun newInstance(
            context: Context,
            withdrawals: Withdrawals
        ): Intent = Intent(context, LocksDetailsActivity::class.java).apply {
            putExtra(LOCK, withdrawals)
        }
    }
}