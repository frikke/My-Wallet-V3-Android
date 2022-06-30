package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.NabuUserSync
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

internal class NabuUserSyncUpdateUserWalletInfoWithJWT(
    private val nabuDataManager: NabuDataManager,
    private val nabuDataUserProvider: NabuDataUserProvider,
) : NabuUserSync {

    override fun syncUser(): Completable =
        Completable.defer {
            nabuDataManager.requestJwt()
                .subscribeOn(Schedulers.io())
                .flatMap { jwt ->
                    nabuDataUserProvider.updateUserWalletInfo(jwt)
                }
                .doOnSuccess {
                    Timber.d(
                        "Syncing nabu user complete, email/phone verified: ${it.emailVerified}, ${it.mobileVerified}"
                    )
                }
                .ignoreElement()
        }
}
