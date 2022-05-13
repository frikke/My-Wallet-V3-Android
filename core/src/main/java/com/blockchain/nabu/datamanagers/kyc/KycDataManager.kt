package com.blockchain.nabu.datamanagers.kyc

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.KycService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.nabu.KycAdditionalInfoNode
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class KycDataManager(
    private val authenticator: Authenticator,
    private val kycService: KycService,
    private val kycAdditionalInfoFeatureFlag: FeatureFlag
) {

    // This is used for testing to we can use this directly rather than using on rxSingleOutcome on the model/presenter
    // which can lead to some issues in tests, specifically on KycMobileValidationPresenterTest we were getting some flakiness
    fun getAdditionalInfoFormSingle(): Single<List<KycAdditionalInfoNode>> =
        rxSingleOutcome { getAdditionalInfoForm() }

    suspend fun getAdditionalInfoForm(): Outcome<KycError, List<KycAdditionalInfoNode>> {
        val isFFEnabled = kycAdditionalInfoFeatureFlag.enabled.await()
        if (!isFFEnabled) return Outcome.Success(emptyList())

        return authenticator.getAuthHeader().awaitOutcome()
            .flatMap { authToken -> kycService.getAdditionalInfoForm(authToken) }
            .mapLeft { KycError.REQUEST_FAILED }
            .map { it?.toDomain() ?: emptyList() }
    }

    suspend fun updateAdditionalInfo(nodes: List<KycAdditionalInfoNode>): Outcome<UpdateKycAdditionalInfoError, Unit> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { UpdateKycAdditionalInfoError.RequestFailed }
            .flatMap { authToken ->
                kycService.updateAdditionalInfo(authToken, nodes.toNetwork())
                    .mapLeft {
                        val nodeId = it.tryParseNodeIdFromApiError()
                        if (nodeId != null) {
                            UpdateKycAdditionalInfoError.InvalidNode(nodeId)
                        } else {
                            UpdateKycAdditionalInfoError.RequestFailed
                        }
                    }
            }

    private fun ApiError.tryParseNodeIdFromApiError(): NodeId? =
        if (this is ApiError.KnownError && errorDescription.count { it == '#' } == 2) {
            val indexOfStart = errorDescription.indexOf('#') + 1
            val indexOfEnd = errorDescription.substring(indexOfStart).indexOf('#')
            errorDescription.substring(indexOfStart, indexOfStart + indexOfEnd)
        } else {
            null
        }
}
