package com.blockchain.coincore.impl.txEngine.staking

import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.txEngine.interest.TransferData
import com.blockchain.core.staking.domain.StakingService
import com.blockchain.core.staking.domain.model.StakingLimits
import com.blockchain.store.asSingle
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Single

private val PendingTx.stakingLimits: StakingLimits
    get() = (this.engineState[STAKING_LIMITS] as StakingLimits)

abstract class StakingBaseEngine(
    private val stakingService: StakingService
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
                    confirmation = TxConfirmation.AGREEMENT_STAKING_TRANSFER,
                    data = TransferData.Staking(
                        pendingTx.amount, pendingTx.stakingLimits
                    ),
                    value = agreementChecked
                )
            )

    protected fun getLimits(): Single<Pair<AssetInfo, StakingLimits>> =
        stakingService.getLimitsForAsset(sourceAssetInfo).asSingle().map { stakingLimits ->
            Pair(sourceAssetInfo, stakingLimits)
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
            TxConfirmation.AGREEMENT_STAKING_TRANSFER
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
        TxConfirmation.AGREEMENT_STAKING_TRANSFER
    )
}
