package com.dex.domain

import kotlinx.coroutines.flow.Flow

interface DexAccountsService {
    fun sourceAccounts(): Flow<List<DexAccount>>
}
