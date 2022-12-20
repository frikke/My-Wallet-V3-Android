package com.blockchain.coreandroid.utils

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Context
import android.content.SharedPreferences

@Suppress("unused")
class CloudBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        val prefs =
            SHARED_PREF_NAME
        SharedPreferencesBackupHelper(this, prefs).also {
            addHelper("prefs", it)
        }
    }

    companion object {
        private const val SHARED_PREF_NAME = "shared_pref_backup"

        fun backupPrefs(ctx: Context): SharedPreferences =
            ctx.applicationContext.getSharedPreferences(SHARED_PREF_NAME, 0)
    }
}
