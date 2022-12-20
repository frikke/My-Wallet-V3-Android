package com.blockchain.core.utils

interface EncryptedPrefs {
    fun hasBackup(): Boolean
    fun backupCurrentPrefs(encryptionKey: String)
    fun restoreFromBackup(decryptionKey: String)
    fun clearBackup()

    var backupEnabled: Boolean
}
