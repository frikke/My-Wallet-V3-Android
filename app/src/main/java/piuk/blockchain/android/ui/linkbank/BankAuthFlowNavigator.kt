package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.core.payments.model.YapilyAttributes
import com.blockchain.core.payments.model.YapilyInstitution
import com.blockchain.core.payments.model.YodleeAttributes
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
    fun yapilyAgreementAccepted(institution: YapilyInstitution)
    fun yapilyApprovalAccepted(approvalDetails: BankPaymentApproval)
    @Deprecated("will be deleted after FF is removed")
    fun yapilyAgreementCancelled(isFromApproval: Boolean)
    fun yapilyAgreementCancelled()
}
