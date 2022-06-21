package com.blockchain.coincore.impl.txEngine.interest

import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.core.interest.domain.InterestStoreService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

abstract class InterestBaseEngine(
    private val walletManager: CustodialWalletManager,
    private val interestStoreService: InterestStoreService,
) : TxEngine() {

    protected val sourceAssetInfo: AssetInfo
        get() = sourceAsset.asAssetInfoOrThrow()

    protected fun modifyEngineConfirmations(
        pendingTx: PendingTx,
        termsChecked: Boolean = getTermsOptionValue(pendingTx),
        agreementChecked: Boolean = getTermsOptionValue(pendingTx)
    ): PendingTx =
        pendingTx.removeOption(TxConfirmation.DESCRIPTION)
            .addOrReplaceOption(
                TxConfirmationValue.TxBooleanConfirmation<Unit>(
                    confirmation = TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
                    value = termsChecked
                )
            )
            .addOrReplaceOption(
                TxConfirmationValue.TxBooleanConfirmation(
                    confirmation = TxConfirmation.AGREEMENT_INTEREST_TRANSFER,
                    data = pendingTx.amount,
                    value = agreementChecked
                )
            )

    protected fun getLimits(): Single<InterestLimits> =
        walletManager.getInterestLimits(sourceAssetInfo)

    protected fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = getTermsOptionValue(pendingTx)
        val agreement = getAgreementOptionValue(pendingTx)
        return (terms && agreement)
    }

    private fun getTermsOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
            TxConfirmation.AGREEMENT_INTEREST_T_AND_C
        )?.value ?: false

    private fun getAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
        TxConfirmation.AGREEMENT_INTEREST_TRANSFER
    )

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable {
        return super.doPostExecute(pendingTx, txResult)
            .doOnComplete { interestStoreService.invalidate() }
    }
}
