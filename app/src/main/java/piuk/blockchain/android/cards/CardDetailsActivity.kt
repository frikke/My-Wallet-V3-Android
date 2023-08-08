package piuk.blockchain.android.cards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.utils.consume
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityCardDetailsBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService

class CardDetailsActivity : BlockchainActivity(), AddCardNavigator, CardDetailsPersistence {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private var cardData: CardData? = null
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val fraudService: FraudService by inject()

    private val binding: ActivityCardDetailsBinding by lazy {
        ActivityCardDetailsBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar { onSupportNavigateUp() }
        if (savedInstanceState == null) {
            navigateToCardDetails()
        }
    }

    override fun showLoading() {
        binding.progress.visible()
    }

    override fun hideLoading() {
        binding.progress.gone()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun restartFlow() {
        setResult(RESULT_CODE_RELAUNCH)
        finish()
    }

    override fun navigateToCardDetails() {
        simpleBuyPrefs.clearCardState()
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(R.id.content_frame, AddNewCardFragment(), AddNewCardFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun navigateToBillingDetails() {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(R.id.content_frame, BillingAddressFragment(), BillingAddressFragment::class.simpleName)
            .addToBackStack(BillingAddressFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun navigateToCardVerification() {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(R.id.content_frame, CardVerificationFragment(), CardVerificationFragment::class.simpleName)
            .addToBackStack(CardVerificationFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun exitWithSuccess(card: PaymentMethod.Card) {
        val data = Intent().apply {
            putExtras(
                Bundle().apply {
                    putSerializable(CARD_KEY, card)
                }
            )
        }
        setResult(RESULT_OK, data)
        finish()
    }

    override fun exitWithError() {
        finish()
    }

    override fun setCardData(cardData: CardData) {
        this.cardData = cardData
    }

    override fun getCardData(): CardData? = cardData

    override fun onDestroy() {
        fraudService.endFlow(FraudFlow.CARD_LINK)
        super.onDestroy()
    }

    companion object {
        const val CARD_KEY = "card_key"
        const val ADD_CARD_REQUEST_CODE = 3245
        const val RESULT_CODE_RELAUNCH = 1234

        fun newIntent(context: Context): Intent = Intent(context, CardDetailsActivity::class.java)
    }
}

data class CardData(val fullName: String, val number: String, val month: Int, val year: Int, val cvv: String)
