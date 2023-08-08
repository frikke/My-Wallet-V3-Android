package com.blockchain.home.presentation.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg

const val ARG_FIAT_TICKER = "fiatTicker"
const val ARG_RECURRING_BUY_ID = "recurringBuyId"
const val ARG_ACTIVITY_TX_ID = "activityTxId"
const val ARG_WALLET_MODE = "walletMode"
const val ARG_IS_FROM_MODE_SWITCH = "isFromModeSwitch"
const val ARG_QUICK_ACTION_VM_KEY = "ARG_QUICK_ACTION_VM_KEY"

sealed class HomeDestination(
    override val route: String
) : ComposeNavigationDestination {
    object CustodialIntro : HomeDestination("CustodialIntro/${ARG_IS_FROM_MODE_SWITCH.wrappedArg()}")
    object DefiIntro : HomeDestination("DefiIntro/${ARG_IS_FROM_MODE_SWITCH.wrappedArg()}")
    object EmailVerification : HomeDestination("EmailVerification")
    object FailedBalances : HomeDestination("FailedBalances")
    object CryptoAssets : HomeDestination("AllAssets")
    object RecurringBuys : HomeDestination("RecurringBuys")
    object RecurringBuyDetail : HomeDestination("RecurringBuyDetail/${ARG_RECURRING_BUY_ID.wrappedArg()}")
    object Activity : HomeDestination("Activity")
    object ActivityDetail : HomeDestination(
        route = "ActivityDetail/${ARG_ACTIVITY_TX_ID.wrappedArg()}/${ARG_WALLET_MODE.wrappedArg()}"
    )

    object Referral : HomeDestination("Referral")
    object SwapDexOptions : HomeDestination("SwapDexOptions")
    object FiatActionDetail : HomeDestination("FiatActionDetail/${ARG_FIAT_TICKER.wrappedArg()}")
    object MoreQuickActions : HomeDestination("MoreQuickActions/${ARG_QUICK_ACTION_VM_KEY.wrappedArg()}")
    object News : HomeDestination("News")

    object KycVerificationPrompt : HomeDestination("KycVerificationPrompt")
}
