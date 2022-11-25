package com.blockchain.coincore.impl.txEngine.interest

import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.earn.domain.models.StakingLimits
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Single

abstract class InterestBaseEngine(
    private val interestService: InterestService
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
                    confirmation = TxConfirmation.AGREEMENT_INTEREST_TRANSFER,
                    data = TransferData.Interest(pendingTx.amount),
                    value = agreementChecked
                )
            )

    protected fun getLimits(): Single<Pair<AssetInfo, InterestLimits>> =
        interestService.getLimitsForAsset(sourceAssetInfo).map { interestLimits ->
            Pair(sourceAssetInfo, interestLimits)
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
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<TransferData>>(
            TxConfirmation.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
        TxConfirmation.AGREEMENT_INTEREST_TRANSFER
    )
}

sealed class TransferData {
    class Interest(val amount: Money) : TransferData()
    class Staking(val amount: Money, val stakingLimits: StakingLimits) : TransferData()
}
