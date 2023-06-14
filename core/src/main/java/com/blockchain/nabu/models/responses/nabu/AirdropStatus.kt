package com.blockchain.nabu.models.responses.nabu

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
    val userState: UserCampaignState? = null,
    val attributes: CampaignAttributes = CampaignAttributes(),
    val updatedAt: @Contextual Date? = null,
    @SerialName("userCampaignTransactionResponseList")
    val txResponseList: List<CampaignTransaction>
)

@Serializable
data class CampaignAttributes(
    @SerialName("x-campaign-address")
    val campaignAddress: String = "",

    @SerialName("x-campaign-code")
    val campaignCode: String = "",

    @SerialName("x-campaign-email")
    val campaignEmail: String = "",

    @SerialName("x-campaign-reject-reason")
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
    val transactionState: CampaignTransactionState
)

@Serializable(with = CampaignStateSerializer::class)
sealed class CampaignState {
    object None : CampaignState()
    object Started : CampaignState()
    object Ended : CampaignState()
}

@Serializable(with = UserCampaignStateSerializer::class)
sealed class UserCampaignState {
    object None : UserCampaignState()
    object Registered : UserCampaignState()
    object TaskFinished : UserCampaignState()
    object RewardSend : UserCampaignState()
    object RewardReceived : UserCampaignState()
    object Failed : UserCampaignState()
}

@Serializable(with = CampaignTransactionStateSerializer::class)
sealed class CampaignTransactionState {
    object None : CampaignTransactionState()
    object PendingDeposit : CampaignTransactionState()
    object FinishedDeposit : CampaignTransactionState()
    object PendingWithdrawal : CampaignTransactionState()
    object FinishedWithdrawal : CampaignTransactionState()
    object Failed : CampaignTransactionState()
}

class UserCampaignStateSerializer : KSerializer<UserCampaignState> {
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
            else -> throw Exception("Unknown UserCampaignState: $input")
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

class CampaignStateSerializer : KSerializer<CampaignState> {
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
            else -> throw Exception("Unknown CampaignState: $input")
        }
    }

    companion object {
        private const val NONE = "NONE"
        private const val STARTED = "STARTED"
        private const val ENDED = "ENDED"
    }
}

class CampaignTransactionStateSerializer : KSerializer<CampaignTransactionState> {
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
            else -> throw Exception("Unknown CampaignTransactionState: $input")
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

const val sunriverCampaignName = "SUNRIVER"
