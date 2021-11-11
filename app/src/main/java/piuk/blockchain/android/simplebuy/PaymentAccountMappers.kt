package piuk.blockchain.android.simplebuy

import android.content.res.Resources
import com.blockchain.nabu.datamanagers.BankAccount
import com.blockchain.nabu.datamanagers.BankDetail
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import piuk.blockchain.android.R

class GBPPaymentAccountMapper(private val resources: Resources) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "GBP") return null
        return BankAccount(
            listOf(
                BankDetail(
                    resources.getString(R.string.account_number),
                    bankAccountResponse.agent.account ?: return null,
                    true
                ),
                BankDetail(
                    resources.getString(R.string.sort_code),
                    bankAccountResponse.agent.code ?: return null,
                    true
                ),
                BankDetail(
                    resources.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: return null
                )
            )
        )
    }
}

class EURPaymentAccountMapper(private val resources: Resources) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "EUR") return null
        return BankAccount(
            listOf(

                BankDetail(
                    resources.getString(R.string.bank_code_swift_bic),
                    bankAccountResponse.agent.account ?: "LHVBEE22",
                    true
                ),

                BankDetail(
                    resources.getString(R.string.bank_name),
                    bankAccountResponse.agent.name ?: return null,
                    true
                ),

                BankDetail(
                    resources.getString(R.string.bank_country),
                    bankAccountResponse.agent.country ?: resources.getString(R.string.estonia)
                ),

                BankDetail(
                    resources.getString(R.string.iban),
                    bankAccountResponse.address ?: return null, true
                ),

                BankDetail(
                    resources.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: ""
                )
            )
        )
    }
}

class USDPaymentAccountMapper(private val resources: Resources) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "USD") return null
        return BankAccount(
            listOfNotNull(
                bankAccountResponse.address?.let { address ->
                    BankDetail(
                        resources.getString(R.string.reference_id_required),
                        address,
                        true
                    )
                },
                BankDetail(
                    resources.getString(R.string.account_number),
                    bankAccountResponse.agent.account ?: "LHVBEE22",
                    true
                ),
                bankAccountResponse.agent.name?.let { name ->
                    BankDetail(
                        resources.getString(R.string.bank_name),
                        name,
                        true
                    )
                },
                bankAccountResponse.agent.accountType?.let { accountType ->
                    BankDetail(
                        resources.getString(R.string.account_type),
                        accountType
                    )
                },
                bankAccountResponse.agent.routingNumber?.let {
                    BankDetail(
                        resources.getString(R.string.routing_number),
                        it, true
                    )
                },

                bankAccountResponse.agent.swiftCode?.let {
                    BankDetail(
                        resources.getString(R.string.bank_code_swift_bic),
                        it, true
                    )
                },

                BankDetail(
                    resources.getString(R.string.bank_country),
                    bankAccountResponse.agent.country ?: resources.getString(R.string.estonia),
                    true
                ),

                bankAccountResponse.agent.address?.let { address ->
                    BankDetail(
                        resources.getString(R.string.bank_address),
                        address,
                        true
                    )
                },
                bankAccountResponse.agent.recipientAddress?.let { address ->
                    BankDetail(
                        resources.getString(R.string.recipient_address),
                        address,
                        true
                    )
                },
                BankDetail(
                    resources.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: "",
                    true
                ),
            )
        )
    }
}
