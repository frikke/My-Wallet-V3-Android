package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.api.getuser.data.store.GetUserDataSource
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

internal class NabuUserSyncUpdateUserWalletInfoWithJWT(
    private val authenticator: NabuAuthenticator,
    private val nabuDataManager: NabuDataManager,
    private val nabuService: NabuService,
    private val userDataSource: GetUserDataSource,
) : NabuUserSync {

    override fun syncUser(): Completable =
        Completable.defer {
            nabuDataManager.requestJwt()
                .subscribeOn(Schedulers.io())
                .flatMap { jwt ->
                    authenticator.authenticate { tokenResponse ->
                        nabuService.updateWalletInformation(tokenResponse, jwt)
                    }.doOnSuccess {
                        userDataSource.invalidate()

                        Timber.d(
                            "Syncing nabu user complete, email/phone verified: %s, %s",
                            it.emailVerified, it.mobileVerified
                        )
                    }
                }
                .ignoreElement()
        }
}
