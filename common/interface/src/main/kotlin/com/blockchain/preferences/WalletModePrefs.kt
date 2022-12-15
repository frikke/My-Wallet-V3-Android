package com.blockchain.preferences

/**
 * When wallet modes introduced the default selecting wallet was CUSTODIAL. This
 * didnt work for all the user/countries as those werent eligible to use the custodial products
 * In order to fix that we introduced the `UseCustodialAccounts` parameter in the products endpoint
 * The logic is the following:
 * If a fresh user opens the app we need to check products and default to Custodial if both available otherwise
 * PKW
 * For an existing user, we need to remember the wallet mode that was used before and select it only if its available,
 * if not the default to the other one. Thats why we need both the legacy and the current, on this interface
 */
interface WalletModePrefs {
    val legacyWalletMode: String
    var currentWalletMode: String
    var userDefaultedToPKW: Boolean
}
