package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.FinishMobileAuthResponse
import com.blockchain.api.kyc.model.PossessionStateResponse
import com.blockchain.api.kyc.model.PrefillDataResponse
import com.blockchain.api.kyc.model.PrefillDataSubmissionRequest
import com.blockchain.api.kyc.model.StartInstantLinkAuthResponse
import com.blockchain.api.kyc.model.StartMobileAuthResponse
import com.blockchain.outcome.Outcome

class ProveApiService(
    private val api: ProveApi
) {

    suspend fun startInstantLinkAuth(
        mobileNumber: String,
    ): Outcome<Exception, StartInstantLinkAuthResponse> =
        api.startInstantLinkAuth(mobileNumber)

    suspend fun startMobileAuth(): Outcome<Exception, StartMobileAuthResponse> =
        api.startMobileAuth()

    suspend fun finishMobileAuth(): Outcome<Exception, FinishMobileAuthResponse> =
        api.finishMobileAuth()

    suspend fun getPossessionState(): Outcome<Exception, PossessionStateResponse> =
        api.getPossessionState()

    suspend fun getPrefillData(
        dob: String, // ISO 8601
    ): Outcome<Exception, PrefillDataResponse> =
        api.getPrefillData(dob)

    suspend fun submitData(
        data: PrefillDataSubmissionRequest,
    ): Outcome<Exception, Unit> =
        api.submitData(data)
}
