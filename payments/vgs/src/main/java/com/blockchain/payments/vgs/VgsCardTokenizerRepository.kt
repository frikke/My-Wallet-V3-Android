package com.blockchain.payments.vgs

import android.content.Context
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.payments.core.CardBillingAddress
import com.verygoodsecurity.vgscollect.core.Environment
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.view.InputFieldView
import com.verygoodsecurity.vgscollect.view.core.serializers.VGSExpDateSeparateSerializer
import com.verygoodsecurity.vgscollect.widget.ExpirationDateEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class VgsCardTokenizerRepository(
    private val environmentConfig: EnvironmentConfig,
    private val json: Json
) : VgsCardTokenizerService {

    private lateinit var vgsForm: VGSCollect

    private var isInitialised: Boolean = false

    override fun init(context: Context, vaultId: String) {
        vgsForm = VGSCollect(
            context = context,
            id = vaultId,
            environment = if (environmentConfig.environment == com.blockchain.enviroment.Environment.STAGING) {
                Environment.SANDBOX
            } else {
                Environment.LIVE
            }
        )
        isInitialised = true
    }

    override fun bindCardDetails(
        name: InputFieldView,
        cardNumber: InputFieldView,
        expiration: ExpirationDateEditText,
        cvv: InputFieldView,
        cardTokenId: String,
    ) {
        with(vgsForm) {
            bindView(name)
            bindView(cardNumber)
            bindView(
                expiration.also {
                    it.setSerializer(
                        VGSExpDateSeparateSerializer(
                            "card_exp.month",
                            "card_exp.year"
                        )
                    )
                }
            )
            bindView(cvv)
            setCustomData(mapOf("bc_card_token_id" to cardTokenId))
        }
    }

    override fun bindAddressDetails(billingAddress: CardBillingAddress) {
        val billingData = mutableMapOf<String, Any>()
        billingData["address"] = mapOf(
            "line1" to billingAddress.addressLine1,
            "line2" to billingAddress.addressLine2,
            "city" to billingAddress.city,
            "state" to (billingAddress.state ?: ""),
            "postCode" to billingAddress.postalCode,
            "country" to billingAddress.country
        )
        vgsForm.setCustomData(billingData)
    }

    override fun isInitialised(): Boolean = isInitialised

    override fun isValid(): Boolean =
        vgsForm.getAllStates().all { it.isValid }

    override suspend fun submit(): String =
        withContext(Dispatchers.IO) {
            val response = vgsForm.submit("/webhook/vgs/tokenize", HTTPMethod.POST)
            requireNotNull(response.body)

            return@withContext json.decodeFromString<VgsResponse>(response.body!!).beneficiaryId
        }

    override fun destroy() {
        if (::vgsForm.isInitialized) {
            vgsForm.onDestroy()
        }
        isInitialised = false
    }
}
