package com.blockchain.coincore.impl.txEngine.active_rewards

import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.txEngine.interest.TransferData
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.data.asSingle
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import com.blockchain.koin.scopedInject
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val PendingTx.arLimits: ActiveRewardsLimits
    get() = (this.engineState[AR_LIMITS] as ActiveRewardsLimits)

abstract class ActiveRewardsBaseEngine(
    private val activeRewardsService: ActiveRewardsService
) : TxEngine(), KoinComponent {

    protected val sourceAssetInfo: AssetInfo
        get() = sourceAsset.asAssetInfoOrThrow()

    override fun ensureSourceBalanceFreshness() {
        activeRewardsService.markBalancesAsStale()
    }

    protected val paymentTransactionHistoryStore: PaymentTransactionHistoryStore by scopedInject()

    private val activeRewardsWithdrawalsFF: FeatureFlag by inject(activeRewardsWithdrawalsFeatureFlag)

    protected fun modifyEngineConfirmations(
        pendingTx: PendingTx,
        termsChecked: Boolean = getTermsOptionValue(pendingTx),
        agreementChecked: Boolean = getAgreementOptionValue(pendingTx)
    ): Single<PendingTx> {
        val confirmationsSingle = Single.just(
            pendingTx.removeOption(TxConfirmation.DESCRIPTION)
                .addOrReplaceOption(
                    TxConfirmationValue.TxBooleanConfirmation<Unit>(
                        confirmation = TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
                        value = termsChecked
                    )
                )
                .addOrReplaceOption(
                    TxConfirmationValue.TxBooleanConfirmation(
                        confirmation = TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER,
                        data = TransferData.ActiveRewards(pendingTx.amount),
                        value = agreementChecked
                    )
                )
        )
        return Single.zip(
            confirmationsSingle,
            activeRewardsWithdrawalsFF.enabled
        ) { confirmations, withdrawalsEnabled ->
            if (withdrawalsEnabled) {
                confirmations
            } else {
                confirmations.addOrReplaceOption(
                    TxConfirmationValue.TxBooleanConfirmation(
                        confirmation = TxConfirmation.AGREEMENT_ACTIVE_REWARDS_WITHDRAWAL_DISABLED,
                        data = TransferData.ActiveRewards(pendingTx.amount),
                        value = getWithdrawalDisabledAgreementOptionValue(pendingTx)
                    )
                )
            }
        }
    }

    protected fun getLimits(): Single<Pair<AssetInfo, ActiveRewardsLimits>> =
        activeRewardsService.getLimitsForAsset(sourceAssetInfo).asSingle().map { arLimits ->
            Pair(sourceAssetInfo, arLimits)
        }

    protected fun areOptionsValid(pendingTx: PendingTx): Single<Boolean> {
        val terms = getTermsOptionValue(pendingTx)
        val agreement = getAgreementOptionValue(pendingTx)

        return activeRewardsWithdrawalsFF.enabled.map { enabled ->
            if (enabled) {
                terms && agreement
            } else {
                val withdrawalDisabledAgreement = getWithdrawalDisabledAgreementOptionValue(pendingTx)
                terms && agreement && withdrawalDisabledAgreement
            }
        }
    }

    private fun getTermsOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
            TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C
        )?.value ?: false

    private fun getAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER
        )?.value ?: false

    private fun getWithdrawalDisabledAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_ACTIVE_REWARDS_WITHDRAWAL_DISABLED
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
        TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER,
        TxConfirmation.AGREEMENT_ACTIVE_REWARDS_WITHDRAWAL_DISABLED
    )
}
