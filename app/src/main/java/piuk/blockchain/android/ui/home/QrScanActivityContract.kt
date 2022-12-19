package piuk.blockchain.android.ui.home

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.blockchain.home.presentation.navigation.QrExpected
import java.net.URLDecoder
import piuk.blockchain.android.ui.scan.QrScanActivity

class QrScanActivityContract : ActivityResultContract<Set<QrExpected>, String>() {
    override fun createIntent(context: Context, input: Set<QrExpected>): Intent {
        return QrScanActivity.newInstance(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String {
        return intent?.getStringExtra(EXTRA_SCAN_RESULT)?.let {
            URLDecoder.decode(it, UTF_8)
        } ?: ""
    }

    companion object {
//        const val SCAN_URI_RESULT = 12007
        const val EXTRA_SCAN_RESULT = "EXTRA_SCAN_RESULT"
        private const val UTF_8 = "UTF-8"
    }
}
