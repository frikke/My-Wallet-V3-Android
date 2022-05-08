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
    LAUNCHER_TO_SPLASH("Launcher->Splash")
}

enum class MomentParam(val value: String) {
    SCREEN_NAME("SCREEN_NAME")
}
