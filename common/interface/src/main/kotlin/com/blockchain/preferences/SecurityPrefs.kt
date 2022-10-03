package com.blockchain.preferences

interface SecurityPrefs {
    val areScreenshotsEnabled: Boolean
    val isUnderTest: Boolean
    var trustScreenOverlay: Boolean
    var disableRootedWarning: Boolean
    fun setScreenshotsEnabled(enable: Boolean)
    fun setIsUnderTest()
}
