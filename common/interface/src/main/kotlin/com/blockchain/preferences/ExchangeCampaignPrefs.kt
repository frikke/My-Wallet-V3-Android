package com.blockchain.preferences

interface ExchangeCampaignPrefs {

    var dismissCount: Int
    var actionTaken: Boolean

    companion object {
        const val DEFAULT_DISMISS_COUNT = 0
        const val DEFAULT_ACTION_TAKEN = false
    }
}
