package com.blockchain.earn.domain.models

import info.blockchain.balance.Money

interface EarnAccountBalance {
    val totalBalance: Money
}
