package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.NullAddress
import com.blockchain.core.payments.model.WithdrawalsLocks
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Money
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.databinding.ViewTxFlowAccountLimitsBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.locks.LocksInfoBottomSheet
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class AccountLimitsView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle),
    EnterAmountWidget, KoinComponent {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics
    private val currencyPrefs: CurrencyPrefs by inject()

    private val binding: ViewTxFlowAccountLimitsBinding =
        ViewTxFlowAccountLimitsBinding.inflate(LayoutInflater.from(context), this, true)

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updatePendingTxDetails(state)
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    private fun updatePendingTxDetails(state: TransactionState) {
        with(binding) {
            customiser.enterAmountLoadSourceIcon(amountSheetLimitsIcon, state)
            amountSheetLimitsDirection.setImageResource(customiser.enterAmountActionIcon(state))
            if (customiser.enterAmountActionIconCustomisation(state)) {
                amountSheetLimitsDirection.setAssetIconColoursWithTint(state.sendingAsset)
            }
        }

        updateSourceAndTargetDetails(state)
    }

    private fun onHoldAmountClicked(locks: WithdrawalsLocks, availableBalance: Money) {
        LocksInfoBottomSheet.newInstance(
            originScreen = LocksInfoBottomSheet.OriginScreenLocks.WITHDRAWAL_SCREEN,
            available = availableBalance.toStringWithSymbol(),
            withdrawalsLocks = locks
        ).show((context as AppCompatActivity).supportFragmentManager, MviFragment.BOTTOM_SHEET)
    }

    private fun updateSourceAndTargetDetails(state: TransactionState) {
        if (state.selectedTarget is NullAddress) {
            return
        }
        with(binding) {
            if (state.action == AssetAction.Withdraw && state.locks != null) {
                infoIcon.visible()
                root.setOnClickListener { onHoldAmountClicked(state.locks, state.availableBalance) }
            }
            amountSheetLimitTitle.text = customiser.enterAmountLimitsViewTitle(state)
            amountSheetLimit.text = customiser.enterAmountLimitsViewInfo(state)
        }
    }
}