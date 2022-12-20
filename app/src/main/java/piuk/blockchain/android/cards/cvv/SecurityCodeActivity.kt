package piuk.blockchain.android.cards.cvv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.extensions.exhaustive
import org.koin.androidx.viewmodel.ext.android.viewModel

class SecurityCodeActivity :
    MVIActivity<SecurityCodeViewState>(),
    NavigationRouter<SecurityCodeNavigation> {

    private val viewModel: SecurityCodeViewModel by viewModel()

    private val paymentId: String
        get() = intent.getStringExtra(PAYMENT_ID).orEmpty()

    private val cardId: String
        get() = intent.getStringExtra(CARD_ID).orEmpty()

    override val alwaysDisableScreenshots: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindViewModel(viewModel, this, SecurityCodeArgs(paymentId = paymentId, cardId = cardId))
        setContent {
            SecurityCodeScreen(
                viewState = viewModel.viewState,
                onIntent = viewModel::onIntent,
                onBackButtonClick = { onBackPressedDispatcher.onBackPressed() }
            )
        }

        onBackPressedDispatcher.addCallback {
            finishUpdatingSecurityCode(false)
        }
    }

    override fun onStateUpdated(state: SecurityCodeViewState) {
        // handled by compose
    }

    override fun route(navigationEvent: SecurityCodeNavigation) {
        when (navigationEvent) {
            is SecurityCodeNavigation.Back -> finishUpdatingSecurityCode(false)
            is SecurityCodeNavigation.Next -> finishUpdatingSecurityCode(true)
            is SecurityCodeNavigation.FinishWithUxError ->
                finishUpdatingSecurityCode(false, navigationEvent.serverSideErrorInfo)
        }.exhaustive
    }

    private fun finishUpdatingSecurityCode(
        hasSecurityCodeBeenUpdated: Boolean,
        serverSideErrorInfo: ServerSideUxErrorInfo? = null,
    ) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(RESULT_UPDATE_SUCCESS, hasSecurityCodeBeenUpdated)
                .putExtra(RESULT_UX_ERROR, serverSideErrorInfo)
        )
        finish()
    }

    companion object {
        const val RESULT_UPDATE_SUCCESS = "RESULT_UPDATE_SUCCESS"
        const val RESULT_UX_ERROR = "RESULT_UX_ERROR"
        private const val PAYMENT_ID = "PAYMENT_ID"
        private const val CARD_ID = "CARD_ID"

        fun newInstance(paymentId: String, cardId: String, context: Context): Intent {
            val intent = Intent(context, SecurityCodeActivity::class.java)
            intent.putExtra(PAYMENT_ID, paymentId)
            intent.putExtra(CARD_ID, cardId)
            return intent
        }
    }

    data class ActivityArgs(val paymentId: String, val cardId: String)

    sealed class ActivityResult {
        object Skipped : ActivityResult()
        object Success : ActivityResult()
        data class Failure(val error: ServerSideUxErrorInfo) : ActivityResult()
    }

    class BlockchainActivityResultContract : ActivityResultContract<ActivityArgs, ActivityResult?>() {
        override fun createIntent(context: Context, input: ActivityArgs): Intent =
            SecurityCodeActivity.newInstance(
                paymentId = input.paymentId,
                cardId = input.cardId,
                context = context
            )

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
            val updateSuccess = intent?.getBooleanExtra(RESULT_UPDATE_SUCCESS, false) ?: false
            val uxError = intent?.getSerializableExtra(RESULT_UX_ERROR) as? ServerSideUxErrorInfo

            return when {
                resultCode != Activity.RESULT_OK -> null
                uxError != null -> ActivityResult.Failure(uxError)
                updateSuccess -> ActivityResult.Success
                else -> ActivityResult.Skipped
            }
        }
    }
}
