package com.blockchain.logging

interface MomentLogger {
    fun startEvent(event: MomentEvent)
    fun endEvent(event: MomentEvent, params: Map<MomentParam, String> = mapOf())
}

enum class MomentEvent(val value: String) {
    /**
     * Ends after the fullscreen loading in portfolio is gone
     */
    PIN_TO_DASHBOARD("Pin->Dashboard"),

    /**
     * Stops in the next screen after splash (login, pin, password required)
     */
    SPLASH_TO_FIRST_SCREEN("Splash->FirstScreen"),

    /**
     * Starts in Application and stops in Splash,
     * to calculate how long it takes to init everything in Application
     */
    LAUNCHER_TO_SPLASH("Launcher->Splash"),

    SWAP_SOURCE_LIST_FF_ON("Swap Source Sort New"),
    SWAP_SOURCE_LIST_FF_OFF("Swap Source Sort Legacy"),
    SWAP_TARGET_LIST_FF_ON("Swap Target Sort New"),
    SWAP_TARGET_LIST_FF_OFF("Swap Target Sort Legacy"),
    SELL_LIST_FF_ON("Sell Sort New"),
    SELL_LIST_FF_OFF("Sell Sort Legacy"),
    DEFAULT_SORTING_CUSTODIAL_ONLY("Default Sort Custodial Only"),
    DEFAULT_SORTING_NC_AND_UNIVERSAL("Default Sort NC & Universal"),
    BUY_LIST_ORDERING_FF_ON("Buy Sort New"),
    BUY_LIST_ORDERING_FF_OFF("Buy Sort Legacy")
}

enum class MomentParam(val value: String) {
    SCREEN_NAME("SCREEN_NAME")
}
