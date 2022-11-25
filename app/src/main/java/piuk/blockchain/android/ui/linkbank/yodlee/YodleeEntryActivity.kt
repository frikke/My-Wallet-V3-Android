package piuk.blockchain.android.ui.linkbank.yodlee

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import piuk.blockchain.android.ui.launcher.LauncherActivityV2

class YodleeEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTaskRoot) {
            startActivity(
                LauncherActivityV2.newInstance(context = applicationContext).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }
        finish()
    }
}
