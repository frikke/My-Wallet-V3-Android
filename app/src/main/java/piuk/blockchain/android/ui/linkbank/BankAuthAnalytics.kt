package piuk.blockchain.android.ui.linkbank

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.extensions.exhaustive
import java.io.Serializable
import java.util.Locale

enum class BankAuthAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    SPLASH_SEEN("sb_bank_link_splash_seen"),
    SPLASH_CTA("sb_bank_link_splash_cont"),
    ACCOUNT_MISMATCH("sb_acc_name_mis_error"),
    ACCOUNT_MISMATCH_RETRY("sb_acc_name_mis_error_try"),
    ACCOUNT_MISMATCH_CANCEL("sb_acc_name_mis_error_cancel"),
    GENERIC_ERROR("sb_bank_link_gen_error"),
    GENERIC_ERROR_RETRY("sb_bank_link_gen_error_try"),
    GENERIC_ERROR_CANCEL("sb_bank_link_gen_error_cancel"),
    ALREADY_LINKED("sb_already_linkd_error"),
    ALREADY_LINKED_RETRY("sb_already_linkd_error_try"),
    ALREADY_LINKED_CANCEL("sb_already_linkd_error_cancel"),
    SUCCESS("sb_bank_link_success"),
    INCORRECT_ACCOUNT("sb_incorrect_acc_error"),
    INCORRECT_ACCOUNT_CANCEL("sb_incorrect_acc_error_cancel"),
    INCORRECT_ACCOUNT_RETRY("sb_incorrect_acc_error_try"),
    SELECT_BANK("ob_select_bank_screen"),
    SELECT_TRANSFER_DETAILS("ob_transfer_details_click"),
    AIS_PERMISSIONS_APPROVED("ob_ais_approve"),
    AIS_PERMISSIONS_DENIED("ob_ais_deny"),
    AIS_EXTERNAL_FLOW_RETRY("ob_ais_retry"),
    PIS_PERMISSIONS_APPROVED("ob_pis_approve"),
    PIS_PERMISSIONS_DENIED("ob_pis_deny"),
    PIS_EXTERNAL_FLOW_RETRY("ob_pis_retry"),
    PIS_EXTERNAL_FLOW_CANCEL("ob_pis_cancel");

    class LinkBankConditionsApproved(
        private val bankName: String,
        private val provider: String,
        private val partner: String,
        override val origin: LaunchOrigin = LaunchOrigin.SETTINGS
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.LINK_BANK_CONDITIONS_APPROVED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "bank_name" to bankName,
                "partner" to partner,
                "provider" to provider
            )
    }

    class LinkBankSelected(override val origin: LaunchOrigin) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.LINK_BANK_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }

    class BankSelected(private val bankName: String, private val provider: String, private val partner: String) :
        AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.BANK_SELECTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "bank_name" to bankName,
                "provider" to provider,
                "partner" to partner
            )
    }
}

private enum class BankPartnerTypes {
    ACH,
    OB;

    companion object {
        fun toAnalyticsType(partner: BankPartner) =
            when (partner) {
                BankPartner.YAPILY -> OB.name
                BankPartner.YODLEE, BankPartner.PLAID -> ACH.name
            }.exhaustive
    }
}

private enum class FlowSource {
    SB,
    DEP,
    WIT,
    SETT;

    companion object {
        fun toAnalyticsType(source: BankAuthSource) =
            when (source) {
                BankAuthSource.SIMPLE_BUY -> SB.name
                BankAuthSource.SETTINGS -> SETT.name
                BankAuthSource.DEPOSIT -> DEP.name
                BankAuthSource.WITHDRAW -> WIT.name
            }
    }
}

fun bankAuthEvent(event: BankAuthAnalytics, source: BankAuthSource): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "flow" to FlowSource.toAnalyticsType(source)
        )
    }

fun bankAuthEvent(event: BankAuthAnalytics, partner: BankPartner): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "partner" to BankPartnerTypes.toAnalyticsType(partner)
        )
    }

fun String.toAnalyticsBankProvider() =
    when {
        this.toLowerCase(Locale.ROOT).contains("safeconnect") -> "SAFE_CONNECT"
        this.toLowerCase(Locale.ROOT).contains("fintecture") -> "FINTECTURE"
        else -> this
    }
