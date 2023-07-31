package com.blockchain.preferences

interface HandholdPrefs {
    var overrideHandholdVerification: Boolean
    var debugHandholdEmailVerified: Boolean
    var debugHandholdKycVerified: Boolean
    var debugHandholdBuyVerified: Boolean
}
