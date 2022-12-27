package piuk.blockchain.android.simplebuy

import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.domain.paymentmethods.model.BankTransferAction
import java.lang.IllegalStateException
import piuk.blockchain.android.BuildConfig

class BankPartnerCallbackProviderImpl : BankPartnerCallbackProvider {
    override fun callback(partner: BankPartner, action: BankTransferAction): String =
        when (partner) {
            BankPartner.YODLEE, BankPartner.PLAID ->
                throw IllegalStateException("Partner $partner doesn't support deeplink callbacks")
            BankPartner.YAPILY ->
                yapilyCallback(action)
        }

    private fun yapilyCallback(action: BankTransferAction): String =
        when (action) {
            BankTransferAction.LINK -> "https://${BuildConfig.DEEPLINK_HOST}/oblinking"
            BankTransferAction.PAY -> "https://${BuildConfig.DEEPLINK_HOST}/obapproval"
        }
}
