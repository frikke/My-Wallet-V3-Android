package com.blockchain.kycproviders.prove.domain

import com.blockchain.kycproviders.prove.domain.model.PossessionState
import com.blockchain.kycproviders.prove.domain.model.PrefillData
import com.blockchain.kycproviders.prove.domain.model.PrefillDataSubmission
import com.blockchain.kycproviders.prove.domain.model.StartInstantLinkAuthResult
import com.blockchain.kycproviders.prove.presentation.ProveAuthResult
import com.blockchain.outcome.Outcome

interface ProveService {

    suspend fun isMobileAuthPossible(): Boolean

    suspend fun verifyPossessionWithMobileAuth(): Outcome<Exception, ProveAuthResult>

    suspend fun startInstantLinkAuth(mobileNumber: String): Outcome<Exception, StartInstantLinkAuthResult>

    suspend fun getPossessionState(): Outcome<Exception, PossessionState>

    suspend fun pollForPossessionVerified(): Outcome<Exception, PossessionState>

    suspend fun getPrefillData(dob: String): Outcome<Exception, PrefillData>

    suspend fun submitData(data: PrefillDataSubmission): Outcome<Exception, Unit>
}
