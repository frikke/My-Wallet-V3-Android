package piuk.blockchain.android.ui.locks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.widget.Toolbar
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityOnHoldDetailsBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.home.ZendeskSubjectActivity
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class LocksDetailsActivity : BlockchainActivity() {

    private val binding: ActivityOnHoldDetailsBinding by lazy {
        ActivityOnHoldDetailsBinding.inflate(layoutInflater)
    }

    private val fundsLocks: FundsLocks by unsafeLazy {
        intent?.getSerializableExtra(KEY_LOCKS) as FundsLocks
    }

    private val userIdentity: NabuUserIdentity by scopedInject()
    private val compositeDisposable = CompositeDisposable()

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
        compositeDisposable += userIdentity.getBasicProfileInformation()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { userInformation ->
                    startActivity(ZendeskSubjectActivity.newInstance(this, userInformation, FUND_LOCKS_SUPPORT))
                }, onError = {
                calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
            }
            )
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
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
