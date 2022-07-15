package piuk.blockchain.android.simplebuy

import android.content.res.Resources
import com.blockchain.nabu.datamanagers.BankAccount
import com.blockchain.nabu.datamanagers.BankDetail
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse.Companion.PARTNER_BIND
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
                    bankAccountResponse.address.formatIBAN() ?: return null, true
                ),

                BankDetail(
                    resources.getString(R.string.recipient_name),
                    bankAccountResponse.agent.recipient ?: ""
                )
            )
        )
    }

    private fun String?.formatIBAN(): String? {
        return this?.replace(Regex(".{4}"), "$0 ")?.trim()
    }
}

class USDPaymentAccountMapper(private val resources: Resources) : PaymentAccountMapper {

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "USD") return null

        return if (bankAccountResponse.partner == PARTNER_BIND) {
            bindUSDBankAccount(bankAccountResponse)
        } else {
            regularUSDBankAccount(bankAccountResponse)
        }
    }

    private fun regularUSDBankAccount(bankAccountResponse: BankAccountResponse) =
        BankAccount(
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
                )
            )
        )

    private fun bindUSDBankAccount(bankAccountResponse: BankAccountResponse) = BankAccount(
        listOfNotNull(
            bankAccountResponse.agent.label?.let { label ->
                BankDetail(
                    title = resources.getString(R.string.alias),
                    value = label,
                    isCopyable = true,
                    tooltip = resources.getString(R.string.alias_tooltip)
                )
            },
            bankAccountResponse.agent.name?.let { accountHolder ->
                BankDetail(
                    title = resources.getString(R.string.account_holder),
                    value = accountHolder,
                    isCopyable = true
                )
            },
            bankAccountResponse.agent.accountType?.let { accountType ->
                BankDetail(
                    title = resources.getString(R.string.account_type),
                    value = accountType,
                    isCopyable = true
                )
            },
            bankAccountResponse.agent.address?.let { address ->
                BankDetail(
                    title = bankAccountResponse.agent.accountType.orEmpty().toBindAccountTypeTitle(),
                    value = address,
                    isCopyable = true
                )
            },
            bankAccountResponse.agent.code?.let { accountNumber ->
                BankDetail(
                    title = resources.getString(R.string.account_number),
                    value = accountNumber,
                    isCopyable = true
                )
            },
            bankAccountResponse.agent.recipient?.let { recipient ->
                BankDetail(
                    title = resources.getString(R.string.recipient_name),
                    value = recipient,
                    isCopyable = true
                )
            }
        )
    )
}

class ARSPaymentAccountMapper(private val resources: Resources) : PaymentAccountMapper {

    enum class AccountType(val value: String) {
        TRADITIONAL("CBU"),
        VIRTUAL("CVU")
    }

    override fun map(bankAccountResponse: BankAccountResponse): BankAccount? {
        if (bankAccountResponse.currency != "ARS") return null
        return BankAccount(
            listOfNotNull(
                bankAccountResponse.agent.bankName?.let { bankName ->
                    BankDetail(
                        title = resources.getString(R.string.bank_name),
                        value = bankName,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.label?.let { label ->
                    BankDetail(
                        title = resources.getString(R.string.alias),
                        value = label,
                        isCopyable = true,
                        tooltip = resources.getString(R.string.alias_tooltip)
                    )
                },
                bankAccountResponse.agent.name?.let { accountHolder ->
                    BankDetail(
                        title = resources.getString(R.string.account_holder),
                        value = accountHolder,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.accountType?.let { accountType ->
                    BankDetail(
                        title = resources.getString(R.string.account_type),
                        value = accountType,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.holderDocument?.let { cuit ->
                    BankDetail(
                        title = resources.getString(R.string.cuit),
                        value = cuit,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.address?.let { address ->
                    BankDetail(
                        title = bankAccountResponse.agent.accountType.orEmpty().toBindAccountTypeTitle(),
                        value = address,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.code?.let { accountNumber ->
                    BankDetail(
                        title = resources.getString(R.string.account_number),
                        value = accountNumber,
                        isCopyable = true
                    )
                },
                bankAccountResponse.agent.recipient?.let { recipient ->
                    BankDetail(
                        title = resources.getString(R.string.recipient_name),
                        value = recipient,
                        isCopyable = true
                    )
                }
            )
        )
    }
}

fun String.toBindAccountTypeTitle() =
    if (this == ARSPaymentAccountMapper.AccountType.TRADITIONAL.value) {
        ARSPaymentAccountMapper.AccountType.TRADITIONAL.value
    } else {
        ARSPaymentAccountMapper.AccountType.VIRTUAL.value
    }
