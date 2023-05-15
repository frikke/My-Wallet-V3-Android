package com.blockchain.kycproviders.prove.data

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.kyc.ProveApiService
import com.blockchain.api.kyc.model.PossessionStateResponse
import com.blockchain.instrumentation.instrument // ktlint-disable instrumentation-ruleset:no-instrumentation-import
import com.blockchain.kycproviders.prove.data.mapper.toDomain
import com.blockchain.kycproviders.prove.data.mapper.toNetwork
import com.blockchain.kycproviders.prove.data.mapper.toPossessionStateDomainOutcome
import com.blockchain.kycproviders.prove.domain.ProveService
import com.blockchain.kycproviders.prove.domain.model.Address
import com.blockchain.kycproviders.prove.domain.model.PossessionState
import com.blockchain.kycproviders.prove.domain.model.PrefillData
import com.blockchain.kycproviders.prove.domain.model.PrefillDataSubmission
import com.blockchain.kycproviders.prove.domain.model.StartInstantLinkAuthResult
import com.blockchain.kycproviders.prove.presentation.ProveAuthResult
import com.blockchain.kycproviders.prove.presentation.ProveAuthSDK
import com.blockchain.network.PollService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProveRepository(
    private val api: ProveApiService,
    private val mobileAuthSDK: ProveAuthSDK
) : ProveService {

    override suspend fun isMobileAuthPossible(): Boolean = instrument(
        "true" to true,
        "false" to false
    ) {
        withContext(Dispatchers.IO) {
            try {
                mobileAuthSDK.isAuthenticationPossible()
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    override suspend fun verifyPossessionWithMobileAuth(): Outcome<Exception, ProveAuthResult> = instrument(
        "success" to Outcome.Success(ProveAuthResult("961234567")),
        "failure" to Outcome.Failure(Exception("Some error"))
    ) {
        withContext(Dispatchers.IO) {
            try {
                val isAuthenticated = mobileAuthSDK.authenticate()

                // TODO(aromano): PROVE check if this case is even possible, for the user to fail possession
                //                as in, prove.com actually determined this phone was compromised
                //  if (!isAuthenticated)
                Outcome.Success(ProveAuthResult("961234567"))
            } catch (ex: Exception) {
                // TODO(aromano): PROVE can this fail?
                Outcome.Failure(ex)
            }
        }
    }

    override suspend fun startInstantLinkAuth(mobileNumber: String): Outcome<Exception, StartInstantLinkAuthResult> =
        instrument(
            "success" to Outcome.Success(StartInstantLinkAuthResult(60)),
            "failure" to Outcome.Failure(Exception("Some error"))
        ) {
            api.startInstantLinkAuth(mobileNumber)
                .map { it.toDomain() }
        }

    override suspend fun getPossessionState(): Outcome<Exception, PossessionState> =
        instrument(
            "verified" to Outcome.Success(PossessionState.Verified("961234567")),
            "unverified" to Outcome.Success(PossessionState.Unverified),
            "failed" to Outcome.Success(PossessionState.Failed),
            "failure error" to Outcome.Failure(Exception("Some error"))
        ) {
            api.getPossessionState().toPossessionStateDomainOutcome()
        }

    override suspend fun pollForPossessionVerified(): Outcome<Exception, PossessionState> = PollService.poll(
        fetch = {
            instrument(
                "verified" to Outcome.Success(PossessionStateResponse(true, "961234567")),
                "unverified" to Outcome.Success(PossessionStateResponse(false, null)),
                "possession failed" to Outcome.Failure(NabuApiException("message", 400, "", 300, "", "", "", null)),
                "failure error" to Outcome.Failure(Exception("Some error"))
            ) {
                api.getPossessionState()
            }
        },
        until = { it.isVerified },
        timerInSec = 3,
        retries = 5 * 60 / 5 // 5 mins / timerInSec
    ).map { it.value }.toPossessionStateDomainOutcome()

    override suspend fun getPrefillData(
        dob: String
    ): Outcome<Exception, PrefillData> =
        instrument(
            "MultipleAddresses" to Outcome.Success(
                PrefillData(
                    firstName = "John",
                    lastName = "Doe",
                    addresses = listOf(address1, address2),
                    dob = "1990-01-20",
                    phoneNumber = "+1-202-555-0100"
                )
            ),
            "OnlyOneAddress" to Outcome.Success(
                PrefillData(
                    firstName = "John",
                    lastName = "Doe",
                    addresses = listOf(address1),
                    dob = "1990-01-20",
                    phoneNumber = "+1-202-555-0100"
                )
            ),
            "NoAddresses" to Outcome.Success(
                PrefillData(
                    firstName = "John",
                    lastName = "Doe",
                    addresses = emptyList(),
                    dob = "1990-01-20",
                    phoneNumber = "+1-202-555-0100"
                )
            ),
            "NoData" to Outcome.Failure(Exception("Some error")),
            "Failure" to Outcome.Failure(Exception("Some error"))
        ) {
            api.getPrefillData(dob)
                .map { it.toDomain() }
        }

    private val address1 = Address(
        line1 = "622 Golden Ridge Road",
        line2 = "19 E",
        city = "Albany",
        state = "AK",
        postCode = "12207",
        country = "US"
    )

    private val address2 = Address(
        line1 = "3288 Custer Street",
        line2 = "Corner Sq",
        city = "Farmerville",
        state = "NY",
        postCode = "71241",
        country = "US"
    )

    override suspend fun submitData(data: PrefillDataSubmission): Outcome<Exception, Unit> =
        instrument(
            "success" to Outcome.Success(Unit),
            "wrong info" to Outcome.Failure(VerificationWrongInfoException),
            "failure" to Outcome.Failure(Exception("Some error"))
        ) {
            api.submitData(data.toNetwork())
                .mapError { error ->
                    if (error is NabuApiException && error.getErrorCode() == NabuErrorCodes.ProveVerificationFailed) {
                        VerificationWrongInfoException
                    } else {
                        error
                    }
                }
        }

    object VerificationWrongInfoException : Exception()
}
