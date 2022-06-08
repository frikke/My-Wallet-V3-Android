package com.blockchain.coincore.loader

import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency

internal object PAX : CryptoCurrency(
    displayTicker = "PAX",
    networkTicker = "PAX",
    name = "Paxos Standard",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l1chainTicker = ETHER.networkTicker,
    l2identifier = "0x8E870D67F660D95d5be530380D0eC0bd388289E1",
    requiredConfirmations = 12, // Same as ETHER
    colour = "#00522C",
    logo = "file:///android_asset/logo/paxos/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
)
