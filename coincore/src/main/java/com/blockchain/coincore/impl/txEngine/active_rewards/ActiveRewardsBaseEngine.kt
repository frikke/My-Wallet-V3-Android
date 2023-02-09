package com.blockchain.coincore.impl.txEngine.active_rewards

import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.txEngine.interest.TransferData
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.store.asSingle
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Single

private val PendingTx.arLimits: ActiveRewardsLimits
    get() = (this.engineState[AR_LIMITS] as ActiveRewardsLimits)

abstract class ActiveRewardsBaseEngine(
    private val activeRewardsService: ActiveRewardsService
) : TxEngine() {

    protected val sourceAssetInfo: AssetInfo
        get() = sourceAsset.asAssetInfoOrThrow()

    protected fun modifyEngineConfirmations(
        pendingTx: PendingTx,
        termsChecked: Boolean = getTermsOptionValue(pendingTx),
        agreementChecked: Boolean = getTermsOptionValue(pendingTx),
    ): PendingTx =
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

    protected fun getLimits(): Single<Pair<AssetInfo, ActiveRewardsLimits>> =
        activeRewardsService.getLimitsForAsset(sourceAssetInfo).asSingle().map { arLimits ->
            Pair(sourceAssetInfo, arLimits)
        }

    protected fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = getTermsOptionValue(pendingTx)
        val agreement = getAgreementOptionValue(pendingTx)
        return (terms && agreement)
    }

    private fun getTermsOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
            TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C
        )?.value ?: false

    private fun getAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
        TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER
    )
}
