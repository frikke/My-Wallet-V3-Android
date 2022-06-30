package piuk.blockchain.android.ui.activity

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType

sealed class ActivitiesIntent : MviIntent<ActivitiesState>

class AccountSelectedIntent(
    val account: BlockchainAccount,
    val isRefreshRequested: Boolean
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        val activitiesList = if (oldState.account == account) {
            oldState.activityList // Is a refresh, keep the list
        } else {
            emptyList()
        }
        return oldState.copy(
            account = account,
            isLoading = true,
            isRefreshRequested = isRefreshRequested,
            activityList = activitiesList
        )
    }
}

object SelectDefaultAccountIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            account = NullCryptoAccount(),
            isLoading = true,
            activityList = emptyList()
        )
    }
}

class ActivityListUpdatedIntent(
    private val activityList: ActivitySummaryList
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            isError = false,
            isLoading = false,
            activityList = activityList
        )
    }
}

object ActivityLoadingIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            isError = false,
            isLoading = true
        )
    }
}

object ActivityListUpdatedErrorIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            isLoading = false,
            activityList = emptyList(),
            isError = true
        )
    }
}

object ShowAccountSelectionIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(bottomSheet = ActivitiesSheet.ACCOUNT_SELECTOR)
    }
}

class CancelSimpleBuyOrderIntent(
    val orderId: String,
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState = oldState
}

class ShowActivityDetailsIntent(
    private val currency: Currency,
    private val txHash: String,
    private val type: ActivityType,
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            bottomSheet =
            if (currency.type == CurrencyType.CRYPTO)
                ActivitiesSheet.CRYPTO_ACTIVITY_DETAILS
            else ActivitiesSheet.FIAT_ACTIVITY_DETAILS,
            selectedCurrency = currency,
            selectedTxId = txHash,
            activityType = type
        )
    }
}

object ClearBottomSheetIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState =
        oldState.copy(
            bottomSheet = null,
            selectedCurrency = null,
            selectedTxId = "",
            activityType = ActivityType.UNKNOWN
        )
}
