package piuk.blockchain.android.ui.transactionflow.flow

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent

class TransactionFlowIntentMapper(
    private val sourceAccount: SingleAccount,
    private val target: TransactionTarget,
    private val action: AssetAction
) {

    fun map(passwordRequired: Boolean) =
        when (action) {
            AssetAction.FiatDeposit -> {
                handleFiatDeposit(passwordRequired)
            }
            AssetAction.Sell -> {
                handleSell(passwordRequired)
            }
            AssetAction.Send -> {
                handleSend(passwordRequired)
            }
            AssetAction.FiatWithdraw -> {
                handleFiatWithdraw(passwordRequired)
            }
            AssetAction.Swap -> {
                handleSwap(passwordRequired)
            }
            AssetAction.InterestDeposit -> {
                handleInterestDeposit(passwordRequired)
            }
            AssetAction.InterestWithdraw -> {
                handleInterestWithdraw(passwordRequired)
            }
            AssetAction.Sign -> {
                handleSignAction(passwordRequired)
            }
            AssetAction.StakingDeposit -> {
                handleStakingDeposit(passwordRequired)
            }
            AssetAction.ActiveRewardsDeposit -> {
                handleActiveRewardsDeposit(passwordRequired)
            }
            AssetAction.ActiveRewardsWithdraw -> {
                handleActiveRewardsWithdraw(passwordRequired)
            }
            AssetAction.Receive,
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Buy -> throw IllegalStateException(
                "Flows for Buy, Receive, ViewActivity and Summary not supported"
            )
        }

    private fun handleSignAction(passwordRequired: Boolean): TransactionIntent =
        TransactionIntent.InitialiseWithSourceAndTargetAccount(
            action,
            sourceAccount,
            target,
            passwordRequired
        )

    private fun handleInterestDeposit(passwordRequired: Boolean) =
        when {
            sourceAccount.isDefinedCryptoAccount() &&
                target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAndTargetAccount(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
            target.isDefinedTarget() -> TransactionIntent.InitialiseWithTargetAndNoSource(
                action = action,
                target = target,
                passwordRequired = passwordRequired
            )
            else -> throw IllegalStateException(
                "Calling interest deposit without source and target is not supported"
            )
        }

    private fun handleStakingDeposit(passwordRequired: Boolean) =
        when {
            sourceAccount.isDefinedCryptoAccount() &&
                target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAndTargetAccount(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
            target.isDefinedTarget() -> TransactionIntent.InitialiseWithTargetAndNoSource(
                action = action,
                target = target,
                passwordRequired = passwordRequired
            )
            else -> throw IllegalStateException(
                "Calling staking deposit without source and target is not supported"
            )
        }

    private fun handleActiveRewardsDeposit(passwordRequired: Boolean) =
        when {
            sourceAccount.isDefinedCryptoAccount() &&
                target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAndTargetAccount(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
            target.isDefinedTarget() -> TransactionIntent.InitialiseWithTargetAndNoSource(
                action = action,
                target = target,
                passwordRequired = passwordRequired
            )
            else -> throw IllegalStateException(
                "Calling active rewards deposit without source and target is not supported"
            )
        }

    private fun handleActiveRewardsWithdraw(passwordRequired: Boolean) =
        when {
            sourceAccount.isDefinedCryptoAccount() && target.isDefinedTarget() ->
                TransactionIntent.InitialiseWithSourceAndTargetAccount(
                    action,
                    sourceAccount,
                    target,
                    passwordRequired
                )
            else -> throw IllegalStateException(
                "Calling active rewards withdraw without source and target is not supported"
            )
        }

    private fun handleInterestWithdraw(passwordRequired: Boolean) =
        when {
            sourceAccount.isDefinedCryptoAccount() -> TransactionIntent.InitialiseWithSourceAccount(
                action,
                sourceAccount,
                passwordRequired
            )
            else -> throw IllegalStateException(
                "Calling interest withdraw without source is not supported"
            )
        }

    private fun handleSwap(passwordRequired: Boolean) =
        when {
            !sourceAccount.isDefinedCryptoAccount() -> TransactionIntent.InitialiseWithNoSourceOrTargetAccount(
                action,
                passwordRequired
            )
            !target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAccount(
                action,
                sourceAccount,
                passwordRequired
            )
            else -> TransactionIntent.InitialiseWithSourceAndPreferredTarget(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
        }

    private fun handleFiatWithdraw(passwordRequired: Boolean): TransactionIntent {
        check(sourceAccount.isFiatAccount())
        return when {
            target is LinkedBankAccount && target.capabilities?.withdrawal?.enabled == false ->
                TransactionIntent.InitialiseWithSourceAccount(
                    action,
                    sourceAccount,
                    passwordRequired
                )
            target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAndPreferredTarget(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
            else -> TransactionIntent.InitialiseWithSourceAccount(
                action,
                sourceAccount,
                passwordRequired
            )
        }
    }

    private fun handleSend(passwordRequired: Boolean): TransactionIntent {
        check(sourceAccount.isDefinedCryptoAccount()) { "Can't start send without a source account" }
        return if (target.isDefinedTarget()) {
            TransactionIntent.InitialiseWithSourceAndTargetAccount(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
        } else {
            TransactionIntent.InitialiseWithSourceAccount(
                action,
                sourceAccount,
                passwordRequired
            )
        }
    }

    private fun handleSell(passwordRequired: Boolean): TransactionIntent =
        when {
            !sourceAccount.isDefinedCryptoAccount() -> TransactionIntent.InitialiseWithNoSourceOrTargetAccount(
                action,
                passwordRequired
            )
            !target.isDefinedTarget() -> TransactionIntent.InitialiseWithSourceAccount(
                action,
                sourceAccount,
                passwordRequired
            )
            else -> throw IllegalStateException("State is not supported")
        }

    private fun handleFiatDeposit(passwordRequired: Boolean): TransactionIntent {
        check(target.isDefinedTarget()) { "Can't deposit without a target" }
        return when {
            sourceAccount.isFiatAccount() -> TransactionIntent.InitialiseWithSourceAndTargetAccount(
                action,
                sourceAccount,
                target,
                passwordRequired
            )
            else -> TransactionIntent.InitialiseWithTargetAndNoSource(
                action,
                target,
                passwordRequired
            )
        }
    }

    private fun BlockchainAccount.isDefinedCryptoAccount() =
        this is CryptoAccount && this !is NullCryptoAccount

    private fun BlockchainAccount.isFiatAccount() =
        this is FiatAccount

    private fun TransactionTarget.isDefinedTarget() =
        this !is NullCryptoAccount
}
