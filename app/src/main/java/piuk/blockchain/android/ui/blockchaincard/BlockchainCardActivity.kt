package piuk.blockchain.android.ui.blockchaincard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddressType
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.googlewallet.manager.GoogleWalletManager
import com.blockchain.blockchaincard.ui.BlockchainCardHostActivity
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.logging.RemoteLogger
import com.blockchain.presentation.koin.scopedInject
import info.blockchain.balance.AssetInfo
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityBlockchainCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

const val GOOGLE_PAY_TOKENIZE_REQUEST_CODE = 101

class BlockchainCardActivity : BlockchainCardHostActivity() {

    override val alwaysDisableScreenshots: Boolean // TODO (labreu): disable screenshots if card number is visible
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val binding: ActivityBlockchainCardBinding by lazy {
        ActivityBlockchainCardBinding.inflate(layoutInflater)
    }

    private val googleWalletManager: GoogleWalletManager by scopedInject()
    private val remoteLogger: RemoteLogger by inject()

    private val blockchainCardProducts by lazy {
        (intent?.getSerializableExtra(BLOCKCHAIN_CARD_PRODUCT_LIST) as? List<BlockchainCardProduct>) ?: emptyList()
    }

    private val blockchainCardList by lazy {
        (intent?.getSerializableExtra(BLOCKCHAIN_CARD_LIST) as? List<BlockchainCard>) ?: emptyList()
    }

    private val preselectedCard by lazy {
        intent?.getParcelableExtra(PRESELECTED_BLOCKCHAIN_CARD) as? BlockchainCard
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList(),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        when {
            blockchainCardList.isNotEmpty() -> {
                startManageCardFlow(
                    blockchainCardProducts = blockchainCardProducts,
                    blockchainCards = blockchainCardList,
                    preselectedCard = preselectedCard,
                    isInitialFlow = true
                )
            }
            blockchainCardProducts.isNotEmpty() -> {
                startOrderCardFlow(isInitialFlow = true)
            }
            else -> {
                throw IllegalStateException("Missing card or product data")
            }
        }
    }

    companion object {
        fun newIntent(
            context: Context,
            blockchainCardProducts: List<BlockchainCardProduct>,
            blockchainCards: List<BlockchainCard>,
            preselectedCard: BlockchainCard? = null
        ): Intent =
            Intent(context, BlockchainCardActivity::class.java).apply {
                putExtra(BLOCKCHAIN_CARD_PRODUCT_LIST, blockchainCardProducts as Serializable)
                putExtra(BLOCKCHAIN_CARD_LIST, blockchainCards as Serializable)
                preselectedCard?.let {
                    putExtra(PRESELECTED_BLOCKCHAIN_CARD, preselectedCard)
                }
            }
    }

    override fun startBuy(asset: AssetInfo) =
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                asset = asset,
                launchFromNavigationBar = true
            )
        )

    override fun startDeposit(account: FiatAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                sourceAccount = NullCryptoAccount(),
                target = account,
                action = AssetAction.FiatDeposit
            )
        )
    }

    override fun startKycAddressVerification(address: BlockchainCardAddress) {
        val fragment = BlockchainCardKycAddressVerificationFragment.newInstance(address)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(binding.blockchainCardContentFrame.id, fragment, fragment::class.simpleName)
            .addToBackStack(fragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun updateKycAddress(address: BlockchainCardAddress) {
        // For billing address, update both viewmodels
        if (address.addressType == BlockchainCardAddressType.BILLING) {
            orderCardViewModel.onIntent(BlockchainCardIntent.UpdateAddress(address))
            manageCardViewModel.onIntent(BlockchainCardIntent.UpdateAddress(address))
        } else {
            orderCardViewModel.onIntent(BlockchainCardIntent.UpdateAddress(address))
        }
    }

    override fun startAddCardToGoogleWallet(pushTokenizeData: BlockchainCardGoogleWalletPushTokenizeData) {
        googleWalletManager.pushTokenizeRequest(this, pushTokenizeData, GOOGLE_PAY_TOKENIZE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_PAY_TOKENIZE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    remoteLogger.logEvent("Google Wallet: card added")
                    manageCardViewModel.onIntent(BlockchainCardIntent.GoogleWalletAddCardSuccess)
                }
                else -> {
                    remoteLogger.logEvent("Google Wallet: add card failed with result code $resultCode")
                    manageCardViewModel.onIntent(BlockchainCardIntent.GoogleWalletAddCardFailed)
                }
            }
        }
    }

    override fun startOrderCardFlow(isInitialFlow: Boolean) {
        if (blockchainCardProducts.isNotEmpty()) {
            val fragment = BlockchainCardFragment.newInstance(blockchainCardProducts = blockchainCardProducts)

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(binding.blockchainCardContentFrame.id, fragment, fragment::class.simpleName)
                .apply {
                    if (!isInitialFlow) {
                        addToBackStack(fragment::class.simpleName)
                    }
                }
                .commitAllowingStateLoss()
        } else {
            Timber.e("No products available for order")
        }
    }

    override fun finishOrderCardFlow() {
        supportFragmentManager.popBackStack()
    }

    override fun orderCardFlowComplete(blockchainCard: BlockchainCard) {
        finishOrderCardFlow()
        startManageCardFlow(blockchainCardProducts, blockchainCardList, blockchainCard)
    }

    override fun startManageCardFlow(
        blockchainCardProducts: List<BlockchainCardProduct>,
        blockchainCards: List<BlockchainCard>,
        preselectedCard: BlockchainCard?,
        isInitialFlow: Boolean
    ) {
        val fragment = BlockchainCardFragment.newInstance(
            blockchainCardProducts = blockchainCardProducts,
            blockchainCards = blockchainCards,
            preselectedCard = preselectedCard
        )

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(binding.blockchainCardContentFrame.id, fragment, fragment::class.simpleName)
            .apply {
                if (!isInitialFlow) {
                    addToBackStack(fragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun openUrl(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )
    }
}
