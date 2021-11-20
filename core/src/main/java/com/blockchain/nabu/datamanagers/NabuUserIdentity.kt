package com.blockchain.nabu.datamanagers

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProvider
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith

class NabuUserIdentity(
    private val custodialWalletManager: CustodialWalletManager,
    private val interestEligibilityProvider: InterestEligibilityProvider,
    private val simpleBuyEligibilityProvider: SimpleBuyEligibilityProvider,
    private val nabuUserDataManager: NabuUserDataManager,
    private val nabuDataProvider: NabuDataUserProvider
) : UserIdentity {
    override fun isEligibleFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> nabuUserDataManager.tiers().map {
                it.isNotInitialisedFor(feature.tier.toKycTierLevel())
            }
            is Feature.SimpleBuy -> simpleBuyEligibilityProvider.isEligibleForSimpleBuy()
            is Feature.Interest -> interestEligibilityProvider.getEligibilityForCustodialAssets()
                .map { assets -> assets.map { it.cryptoCurrency }.contains(feature.currency) }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.isSimplifiedDueDiligenceEligible()
        }.exhaustive
    }

    override fun isVerifiedFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> nabuUserDataManager.tiers().map {
                it.isApprovedFor(feature.tier.toKycTierLevel())
            }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.fetchSimplifiedDueDiligenceUserState().map {
                it.isVerified
            }
            is Feature.SimpleBuy,
            is Feature.Interest -> throw IllegalArgumentException("Cannot be verified for $feature")
        }.exhaustive
    }

    override fun isRejectedForTier(feature: Feature.TierLevel): Single<Boolean> {
        return nabuUserDataManager.tiers().map {
            it.isRejectedFor(feature.tier.toKycTierLevel())
        }
    }

    override fun isKycInProgress(): Single<Boolean> =
        nabuDataProvider
            .getUser()
            .map { it.tiers?.next ?: 0 }
            .zipWith(nabuUserDataManager.tiers())
            .map { (user, tiers) ->
                tiers.isNotInitialisedFor(KycTierLevel.values()[user])
            }

    override fun getBasicProfileInformation(): Single<BasicProfileInfo> =
        nabuDataProvider.getUser().map {
            it.toBasicProfileInfo()
        }

    override fun getUserCountry(): Maybe<String> =
        nabuDataProvider.getUser().flatMapMaybe {
            val countryCode = it.address?.countryCode
            if (countryCode.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(countryCode)
            }
        }

    override fun getUserState(): Maybe<String> =
        nabuDataProvider.getUser().flatMapMaybe {
            val state = it.address?.state
            if (state.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(state)
            }
        }

    private fun NabuUser.toBasicProfileInfo() =
        BasicProfileInfo(
            firstName = firstName ?: email,
            lastName = lastName ?: email,
            email = email
        )

    override fun isKycResubmissionRequired(): Single<Boolean> =
        nabuDataProvider.getUser().map { it.isMarkedForResubmission }

    override fun shouldResubmitAfterRecovery(): Single<Boolean> =
        nabuDataProvider.getUser().map { it.isMarkedForRecoveryResubmission }

    override fun checkForUserWalletLinkErrors(): Completable =
        nabuDataProvider.getUser().flatMapCompletable { Completable.complete() }

    private fun Tier.toKycTierLevel(): KycTierLevel =
        when (this) {
            Tier.BRONZE -> KycTierLevel.BRONZE
            Tier.SILVER -> KycTierLevel.SILVER
            Tier.GOLD -> KycTierLevel.GOLD
        }.exhaustive
}
