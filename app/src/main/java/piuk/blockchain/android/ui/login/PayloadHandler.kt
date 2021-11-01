package piuk.blockchain.android.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import piuk.blockchain.android.ui.login.auth.LoginAuthActivity

class PayloadHandler {

    companion object {
        fun getDataFromIntent(intent: Intent): String? =
            intent.data?.fragment?.substringAfterLast(LoginAuthActivity.LINK_DELIMITER)

        fun getDataFromUri(uri: Uri): String? =
            uri.fragment?.substringAfterLast(LoginAuthActivity.LINK_DELIMITER)

        fun decodeToJsonString(payload: String): String {
            val urlSafeEncodedData = payload.apply {
                unEscapedCharactersMap.map { entry ->
                    replace(entry.key, entry.value)
                }
            }
            val decodedData = tryDecode(urlSafeEncodedData.toByteArray(Charsets.UTF_8))
            return String(decodedData)
        }

        private fun tryDecode(urlSafeEncodedData: ByteArray): ByteArray {
            return try {
                Base64.decode(
                    urlSafeEncodedData,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            } catch (ex: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // The getUrlDecoder() returns the URL_SAFE Base64 decoder
                    java.util.Base64.getUrlDecoder().decode(urlSafeEncodedData)
                } else {
                    throw ex
                }
            }
        }

        private val unEscapedCharactersMap = mapOf(
            "%2B" to "+",
            "%2F" to "/",
            "%2b" to "+",
            "%2f" to "/"
        )
    }
}