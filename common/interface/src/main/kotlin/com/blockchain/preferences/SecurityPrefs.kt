package com.blockchain.preferences

interface SecurityPrefs {
    val areScreenshotsEnabled: Boolean
    val isUnderTest: Boolean
    var trustScreenOverlay: Boolean
    fun setScreenshotsEnabled(enable: Boolean)
    fun setIsUnderTest()
}
