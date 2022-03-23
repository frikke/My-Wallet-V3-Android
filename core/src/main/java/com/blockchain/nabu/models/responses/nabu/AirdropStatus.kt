package com.blockchain.nabu.models.responses.nabu

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class AirdropStatusList(
    private val userCampaignsInfoResponseList: List<AirdropStatus> = emptyList()
) {
    operator fun get(campaignName: String): AirdropStatus? {
        return userCampaignsInfoResponseList.firstOrNull { it.campaignName == campaignName }
    }

    val airdropList
        get() = userCampaignsInfoResponseList
}

@Serializable
data class AirdropStatus(
    val campaignName: String,
    val campaignEndDate: @Contextual Date? = null, // NOT USED!
    val campaignState: CampaignState,
    @SerialName("userCampaignState")
    @field:Json(name = "userCampaignState")
    val userState: UserCampaignState,
    val attributes: CampaignAttributes = CampaignAttributes(),
    val updatedAt: @Contextual Date,
    @SerialName("userCampaignTransactionResponseList")
    @field:Json(name = "userCampaignTransactionResponseList")
    val txResponseList: List<CampaignTransaction>
)

@Serializable
data class CampaignAttributes(
    @SerialName("x-campaign-address")
    @field:Json(name = "x-campaign-address")
    val campaignAddress: String = "",

    @SerialName("x-campaign-code")
    @field:Json(name = "x-campaign-code")
    val campaignCode: String = "",

    @SerialName("x-campaign-email")
    @field:Json(name = "x-campaign-email")
    val campaignEmail: String = "",

    @SerialName("x-campaign-reject-reason")
    @field:Json(name = "x-campaign-reject-reason")
    val rejectReason: String = ""
)

@Serializable
data class CampaignTransaction(
    val fiatValue: Long,
    val fiatCurrency: String,
    val withdrawalQuantity: Long,
    val withdrawalCurrency: String,
    val withdrawalAt: @Contextual Date,
    @SerialName("userCampaignTransactionState")
    @field:Json(name = "userCampaignTransactionState")
    val transactionState: CampaignTransactionState
)

@Serializable(with = CampaignStateMoshiAdapter.CampaignStateSerializer::class)
sealed class CampaignState {
    object None : CampaignState()
    object Started : CampaignState()
    object Ended : CampaignState()
}

@Serializable(with = UserCampaignStateMoshiAdapter.UserCampaignStateSerializer::class)
sealed class UserCampaignState {
    object None : UserCampaignState()
    object Registered : UserCampaignState()
    object TaskFinished : UserCampaignState()
    object RewardSend : UserCampaignState()
    object RewardReceived : UserCampaignState()
    object Failed : UserCampaignState()
}

@Serializable(with = CampaignTransactionStateMoshiAdapter.CampaignTransactionStateSerializer::class)
sealed class CampaignTransactionState {
    object None : CampaignTransactionState()
    object PendingDeposit : CampaignTransactionState()
    object FinishedDeposit : CampaignTransactionState()
    object PendingWithdrawal : CampaignTransactionState()
    object FinishedWithdrawal : CampaignTransactionState()
    object Failed : CampaignTransactionState()
}

// -------------------------------------------------------------------------------------------------------
// Moshi JSON adapters

@Deprecated("Use [IsoDateSerializer] instead.")
class IsoDateMoshiAdapter {

    @SuppressLint("SimpleDateFormat")
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // ISO-8601 date format

    @FromJson
    fun fromJson(input: String): Date = format.parse(input)

    @ToJson
    fun toJson(date: Date): String = format.format(date)
}

@Deprecated("Use [UserCampaignStateSerializer] instead.")
// TODO Remove Moshi and migrate UserCampaignStateMoshiAdapter to UserCampaignStateSerializer
class UserCampaignStateMoshiAdapter {
    @FromJson
    fun fromJson(input: String): UserCampaignState =
        when (input) {
            NONE -> UserCampaignState.None
            REGISTERED -> UserCampaignState.Registered
            TASK_FINISHED -> UserCampaignState.TaskFinished
            REWARD_SEND -> UserCampaignState.RewardSend
            REWARD_RECEIVED -> UserCampaignState.RewardReceived
            FAILED -> UserCampaignState.Failed
            else -> throw JsonDataException("Unknown UserCampaignState: $input")
        }

    @ToJson
    fun toJson(state: UserCampaignState): String =
        when (state) {
            UserCampaignState.None -> NONE
            UserCampaignState.Registered -> REGISTERED
            UserCampaignState.TaskFinished -> TASK_FINISHED
            UserCampaignState.RewardSend -> REWARD_SEND
            UserCampaignState.RewardReceived -> REWARD_RECEIVED
            UserCampaignState.Failed -> FAILED
        }

    object UserCampaignStateSerializer : KSerializer<UserCampaignState> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("UserCampaignState", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UserCampaignState) {
            encoder.encodeString(
                when (value) {
                    UserCampaignState.None -> NONE
                    UserCampaignState.Registered -> REGISTERED
                    UserCampaignState.TaskFinished -> TASK_FINISHED
                    UserCampaignState.RewardSend -> REWARD_SEND
                    UserCampaignState.RewardReceived -> REWARD_RECEIVED
                    UserCampaignState.Failed -> FAILED
                }
            )
        }

        override fun deserialize(decoder: Decoder): UserCampaignState {
            return when (val input = decoder.decodeString()) {
                NONE -> UserCampaignState.None
                REGISTERED -> UserCampaignState.Registered
                TASK_FINISHED -> UserCampaignState.TaskFinished
                REWARD_SEND -> UserCampaignState.RewardSend
                REWARD_RECEIVED -> UserCampaignState.RewardReceived
                FAILED -> UserCampaignState.Failed
                else -> throw JsonDataException("Unknown UserCampaignState: $input")
            }
        }
    }

    companion object {
        private const val NONE = "NONE"
        private const val REGISTERED = "REGISTERED"
        private const val TASK_FINISHED = "TASK_FINISHED"
        private const val REWARD_SEND = "REWARD_SEND"
        private const val REWARD_RECEIVED = "REWARD_RECEIVED"
        private const val FAILED = "FAILED"
    }
}

@Deprecated("Use [CampaignStateSerializer] instead.")
// TODO Remove Moshi and migrate CampaignStateMoshiAdapter to CampaignStateSerializer
class CampaignStateMoshiAdapter {
    @FromJson
    fun fromJson(input: String): CampaignState =
        when (input) {
            NONE -> CampaignState.None
            STARTED -> CampaignState.Started
            ENDED -> CampaignState.Ended
            else -> throw JsonDataException("Unknown CampaignState: $input")
        }

    @ToJson
    fun toJson(state: CampaignState): String =
        when (state) {
            CampaignState.None -> NONE
            CampaignState.Started -> STARTED
            CampaignState.Ended -> ENDED
        }

    object CampaignStateSerializer : KSerializer<CampaignState> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CampaignState", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: CampaignState) {
            encoder.encodeString(
                when (value) {
                    CampaignState.None -> NONE
                    CampaignState.Started -> STARTED
                    CampaignState.Ended -> ENDED
                }
            )
        }

        override fun deserialize(decoder: Decoder): CampaignState {
            return when (val input = decoder.decodeString()) {
                NONE -> CampaignState.None
                STARTED -> CampaignState.Started
                ENDED -> CampaignState.Ended
                else -> throw JsonDataException("Unknown CampaignState: $input")
            }
        }
    }

    companion object {
        private const val NONE = "NONE"
        private const val STARTED = "STARTED"
        private const val ENDED = "ENDED"
    }
}

@Deprecated("Use [CampaignTransactionStateSerializer] instead.")
// TODO Remove Moshi and migrate CampaignTransactionStateMoshiAdapter to CampaignTransactionStateSerializer
class CampaignTransactionStateMoshiAdapter {

    @FromJson
    fun fromJson(input: String): CampaignTransactionState =
        when (input) {
            NONE -> CampaignTransactionState.None
            PENDING_DEPOSIT -> CampaignTransactionState.PendingDeposit
            FINISHED_DEPOSIT -> CampaignTransactionState.FinishedDeposit
            PENDING_WITHDRAWAL -> CampaignTransactionState.PendingWithdrawal
            FINISHED_WITHDRAWAL -> CampaignTransactionState.FinishedWithdrawal
            FAILED -> CampaignTransactionState.Failed
            else -> throw JsonDataException("Unknown CampaignTransactionState: $input")
        }

    @ToJson
    fun toJson(state: CampaignTransactionState): String =
        when (state) {
            CampaignTransactionState.None -> NONE
            CampaignTransactionState.PendingDeposit -> PENDING_DEPOSIT
            CampaignTransactionState.FinishedDeposit -> FINISHED_DEPOSIT
            CampaignTransactionState.PendingWithdrawal -> PENDING_WITHDRAWAL
            CampaignTransactionState.FinishedWithdrawal -> FINISHED_WITHDRAWAL
            CampaignTransactionState.Failed -> FAILED
        }

    object CampaignTransactionStateSerializer : KSerializer<CampaignTransactionState> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CampaignTransactionState", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: CampaignTransactionState) {
            encoder.encodeString(
                when (value) {
                    CampaignTransactionState.None -> NONE
                    CampaignTransactionState.PendingDeposit -> PENDING_DEPOSIT
                    CampaignTransactionState.FinishedDeposit -> FINISHED_DEPOSIT
                    CampaignTransactionState.PendingWithdrawal -> PENDING_WITHDRAWAL
                    CampaignTransactionState.FinishedWithdrawal -> FINISHED_WITHDRAWAL
                    CampaignTransactionState.Failed -> FAILED
                }
            )
        }

        override fun deserialize(decoder: Decoder): CampaignTransactionState {
            return when (val input = decoder.decodeString()) {
                NONE -> CampaignTransactionState.None
                PENDING_DEPOSIT -> CampaignTransactionState.PendingDeposit
                FINISHED_DEPOSIT -> CampaignTransactionState.FinishedDeposit
                PENDING_WITHDRAWAL -> CampaignTransactionState.PendingWithdrawal
                FINISHED_WITHDRAWAL -> CampaignTransactionState.FinishedWithdrawal
                FAILED -> CampaignTransactionState.Failed
                else -> throw JsonDataException("Unknown CampaignTransactionState: $input")
            }
        }
    }

    companion object {
        private const val NONE = "NONE"
        private const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        private const val FINISHED_DEPOSIT = "FINISHED_DEPOSIT"
        private const val PENDING_WITHDRAWAL = "PENDING_WITHDRAWAL"
        private const val FINISHED_WITHDRAWAL = "FINISHED_WITHDRAWAL"
        private const val FAILED = "FAILED"
    }
}
