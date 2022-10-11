package com.blockchain.nabu.datamanagers

import com.blockchain.logging.Logger
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers

internal class NabuUserSyncUpdateUserWalletInfoWithJWT(
    private val nabuDataManager: NabuDataManager,
    private val nabuService: NabuService,
    private val getUserStore: GetUserStore,
) : NabuUserSync {

    override fun syncUser(): Completable =
        Completable.defer {
            nabuDataManager.requestJwt()
                .subscribeOn(Schedulers.io())
                .flatMap { jwt ->
                    nabuService.updateWalletInformation(jwt)
                        .doOnSuccess {
                            getUserStore.invalidate()

                            Logger.d(
                                "Syncing nabu user complete, email/phone verified: %s, %s",
                                it.emailVerified, it.mobileVerified
                            )
                        }
                }
                .ignoreElement()
        }
}
