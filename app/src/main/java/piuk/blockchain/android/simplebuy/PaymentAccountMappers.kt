package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.BankAccount
import com.blockchain.nabu.datamanagers.BankDetail
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils

class GBPPaymentAccountMapper(private val stringUtils: StringUtils) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "GBP") return null
        return BankAccount(
            listOf(
                BankDetail(
                    stringUtils.getString(R.string.account_number),
                    bankAccountResponse.agent.account ?: return null,
                    true
                ),
                BankDetail(
                    stringUtils.getString(R.string.sort_code),
                    bankAccountResponse.agent.code ?: return null,
                    true
                ),
                BankDetail(
                    stringUtils.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: return null
                )
            )
        )
    }
}

class EURPaymentAccountMapper(private val stringUtils: StringUtils) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "EUR") return null
        return BankAccount(
            listOf(

                BankDetail(
                    stringUtils.getString(R.string.bank_code_swift_bic),
                    bankAccountResponse.agent.account ?: "LHVBEE22",
                    true
                ),

                BankDetail(
                    stringUtils.getString(R.string.bank_name),
                    bankAccountResponse.agent.name ?: return null,
                    true
                ),

                BankDetail(
                    stringUtils.getString(R.string.bank_country),
                    bankAccountResponse.agent.country ?: stringUtils.getString(R.string.estonia)
                ),

                BankDetail(
                    stringUtils.getString(R.string.iban),
                    bankAccountResponse.address ?: return null, true
                ),

                BankDetail(
                    stringUtils.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: ""
                )
            )
        )
    }
}

class USDPaymentAccountMapper(private val stringUtils: StringUtils) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "USD") return null
        return BankAccount(
            listOfNotNull(
                bankAccountResponse.address?.let { address ->
                    BankDetail(
                        stringUtils.getString(R.string.reference_id_required),
                        address,
                        true
                    )
                },
                BankDetail(
                    stringUtils.getString(R.string.account_number),
                    bankAccountResponse.agent.account ?: "LHVBEE22",
                    true
                ),
                bankAccountResponse.agent.name?.let { name ->
                    BankDetail(
                        stringUtils.getString(R.string.bank_name),
                        name,
                        true
                    )
                },
                bankAccountResponse.agent.accountType?.let { accountType ->
                    BankDetail(
                        stringUtils.getString(R.string.account_type),
                        accountType
                    )
                },
                bankAccountResponse.agent.routingNumber?.let {
                    BankDetail(
                        stringUtils.getString(R.string.routing_number),
                        it, true
                    )
                },

                bankAccountResponse.agent.swiftCode?.let {
                    BankDetail(
                        stringUtils.getString(R.string.bank_code_swift_bic),
                        it, true
                    )
                },

                BankDetail(
                    stringUtils.getString(R.string.bank_country),
                    bankAccountResponse.agent.country ?: stringUtils.getString(R.string.estonia)
                ),

                bankAccountResponse.agent.address?.let { address ->
                    BankDetail(
                        stringUtils.getString(R.string.bank_address),
                        address
                    )
                },
                bankAccountResponse.agent.recipientAddress?.let { address ->
                    BankDetail(
                        stringUtils.getString(R.string.recipient_address),
                        address
                    )
                },
                BankDetail(
                    stringUtils.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: ""
                ),
            )
        )
    }
}
