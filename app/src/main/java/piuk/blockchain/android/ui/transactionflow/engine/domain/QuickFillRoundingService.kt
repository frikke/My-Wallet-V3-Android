package piuk.blockchain.android.ui.transactionflow.engine.domain

import com.blockchain.coincore.AssetAction
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.transactionflow.engine.domain.model.QuickFillRoundingData

interface QuickFillRoundingService {
    fun getQuickFillRoundingForAction(action: AssetAction): Single<List<QuickFillRoundingData>>
}
