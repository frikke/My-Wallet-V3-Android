package piuk.blockchain.android.ui.linkbank

import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import info.blockchain.balance.FiatCurrency

interface BankAuthFlowNavigator {
    fun launchYodleeSplash(attributes: YodleeAttributes, bankId: String)
    fun launchYodleeWebview(attributes: YodleeAttributes, bankId: String)
    fun launchBankLinking(accountProviderId: String, accountId: String, bankId: String)
    fun retry()
    fun bankLinkingFinished(bankId: String, currency: FiatCurrency)
    fun bankAuthCancelled()
    fun launchYapilyBankSelection(attributes: YapilyAttributes)
    fun showTransferDetails()
    fun yapilyInstitutionSelected(institution: YapilyInstitution, entity: String)
    fun launchPlaidLink(attributes: PlaidAttributes, id: String)

    @Deprecated("will be deleted after FF is removed")
    fun yapilyAgreementAccepted(institution: YapilyInstitution)
    fun yapilyApprovalAccepted(approvalDetails: BankPaymentApproval)

    @Deprecated("will be deleted after FF is removed")
    fun yapilyAgreementCancelled(isFromApproval: Boolean)
}
