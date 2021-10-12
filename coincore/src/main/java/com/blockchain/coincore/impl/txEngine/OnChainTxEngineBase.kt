package com.blockchain.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.AssetInfo
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeState
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.koin.scopedInject
import org.koin.core.component.inject
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

abstract class OnChainTxEngineBase(
    override val requireSecondPassword: Boolean,
    private val walletPreferences: WalletStatus
) : TxEngine() {

    private val settingsDataManager: SettingsDataManager by scopedInject()
    private val prefs: PersistentPrefs by inject()

    override fun assertInputsValid() {
        val tgt = txTarget
        check(tgt is CryptoAddress)
        check(tgt.address.isNotEmpty())
        check(sourceAsset == tgt.asset)
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        txTarget.onTxCompleted(txResult)
            .doOnComplete {
                settingsDataManager.triggerOnChainTransaction(
                    guid = prefs.walletGuid,
                    sharedKey = prefs.sharedKey,
                    amount = pendingTx.amount.toStringWithoutSymbol(),
                    currency = pendingTx.amount.currencyCode
                )
            }

    protected fun mapSavedFeeToFeeLevel(feeType: Int?): FeeLevel =
        when (feeType) {
            FeeLevel.Priority.ordinal -> FeeLevel.Priority
            FeeLevel.Regular.ordinal -> FeeLevel.Regular
            else -> FeeLevel.Regular
        }

    private fun FeeLevel.mapFeeLevelToSavedValue() =
        this.ordinal

    private fun storeDefaultFeeLevel(cryptoCurrency: AssetInfo, feeLevel: FeeLevel) =
        walletPreferences.setFeeTypeForAsset(cryptoCurrency, feeLevel.mapFeeLevelToSavedValue())

    protected fun fetchDefaultFeeLevel(cryptoCurrency: AssetInfo): Int? =
        walletPreferences.getFeeTypeForAsset(cryptoCurrency)

    protected fun getFeeState(pTx: PendingTx, feeOptions: FeeOptions? = null) =
        if (pTx.feeSelection.selectedLevel == FeeLevel.Custom) {
            when {
                pTx.feeSelection.customAmount == -1L -> FeeState.ValidCustomFee
                pTx.availableBalance < pTx.amount -> FeeState.FeeTooHigh
                pTx.feeSelection.customAmount < MINIMUM_CUSTOM_FEE -> {
                    FeeState.FeeUnderMinLimit
                }
                pTx.feeSelection.customAmount >= MINIMUM_CUSTOM_FEE &&
                    pTx.feeSelection.customAmount <= feeOptions?.limits?.min ?: 0L -> {
                    FeeState.FeeUnderRecommended
                }
                pTx.feeSelection.customAmount >= feeOptions?.limits?.max ?: 0L -> {
                    FeeState.FeeOverRecommended
                }
                else -> FeeState.ValidCustomFee
            }
        } else {
            if (pTx.availableBalance < pTx.amount) {
                FeeState.FeeTooHigh
            } else {
                FeeState.FeeDetails(pTx.feeAmount)
            }
        }

    final override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))

        return if (pendingTx.hasFeeLevelChanged(level, customFeeAmount)) {
            updateFeeSelection(
                sourceAsset,
                pendingTx,
                level,
                customFeeAmount
            )
        } else {
            Single.just(pendingTx)
        }
    }

    private fun updateFeeSelection(
        cryptoCurrency: AssetInfo,
        pendingTx: PendingTx,
        newFeeLevel: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        storeDefaultFeeLevel(cryptoCurrency, newFeeLevel)

        return doUpdateAmount(
            amount = pendingTx.amount,
            pendingTx = pendingTx.copy(
                feeSelection = pendingTx.feeSelection.copy(
                    selectedLevel = newFeeLevel,
                    customAmount = customFeeAmount
                )
            )
        )
    }

    private fun PendingTx.hasFeeLevelChanged(newLevel: FeeLevel, newAmount: Long) =
        with(feeSelection) {
            selectedLevel != newLevel || (selectedLevel == FeeLevel.Custom && newAmount != customAmount)
        }

    companion object {
        const val MINIMUM_CUSTOM_FEE = 1L
    }
}
