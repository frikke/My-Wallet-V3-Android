package piuk.blockchain.android.everypay.service

import com.blockchain.utils.withBearerPrefix
import io.reactivex.rxjava3.core.Single
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import piuk.blockchain.android.everypay.models.CardDetailRequest
import piuk.blockchain.android.everypay.models.CardDetailResponse
import piuk.blockchain.android.everypay.models.CcDetails

class EveryPayCardService(
    private val everyPayService: EveryPayService
) {
    fun submitCard(cardDetails: CcDetails, apiUserName: String, mobileToken: String): Single<CardDetailResponse> =
        everyPayService.getCardDetail(
            CardDetailRequest(
                apiUsername = apiUserName,
                mobileToken = mobileToken,
                nonce = nonce(),
                timestamp = timestamp(),
                ccDetails = cardDetails
            ),
            mobileToken.withBearerPrefix()
        )

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date())

    private fun nonce(): String {
        val source = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val len = 30
        val sb = StringBuilder(len)
        val random = Random()

        for (i in 0 until len) {
            sb.append(source[random.nextInt(source.length)])
        }
        return sb.toString()
    }
}
