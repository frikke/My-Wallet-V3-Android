package piuk.blockchain.android.ui.kyc.profile

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.util.toISO8601DateString
import com.google.common.base.Optional
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

class KycProfilePresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val userService: UserService,
    private val stringUtils: StringUtils,
) : BaseKycPresenter<KycProfileView>(nabuToken) {

    var firstNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var lastNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var dateSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }

    override fun onViewReady() {
        restoreDataIfPresent()
    }

    internal fun onContinueClicked(campaignType: CampaignType? = null) {
        check(view.firstName.isNotEmpty()) { "firstName is empty" }
        check(view.lastName.isNotEmpty()) { "lastName is empty" }
        check(view.dateOfBirth != null) { "dateOfBirth is null" }

        compositeDisposable += fetchOfflineToken.flatMapCompletable {
            createBasicUser(it)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onComplete = {
                    ProfileModel(
                        firstName = view.firstName,
                        lastName = view.lastName,
                        countryCode = view.countryCode,
                        stateCode = view.stateCode,
                    ).run { view.continueSignUp(this) }
                },
                onError = {
                    if (it is NabuApiException &&
                        it.getErrorStatusCode() == NabuErrorStatusCodes.Conflict
                    ) {
                        view.showErrorSnackbar(stringUtils.getString(R.string.kyc_profile_error_conflict))
                    } else {
                        view.showErrorSnackbar(stringUtils.getString(R.string.kyc_profile_error))
                    }
                }
            )
    }

    private fun restoreDataIfPresent() {
        // Don't restore data if data already present, as it'll overwrite what the user
        // may have edited themselves
        if (!firstNameSet && !lastNameSet && !dateSet) {
            compositeDisposable +=
                userService.getUser()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = {
                            val firstName = it.firstName ?: return@subscribeBy
                            val lastName = it.lastName ?: return@subscribeBy
                            val dob = it.dob ?: return@subscribeBy
                            val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                            val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val displayDate = backendFormat.parse(dob)

                            view.restoreUiState(
                                firstName,
                                lastName,
                                displayFormat.format(displayDate),
                                displayDate.toCalendar()
                            )
                        },
                        onError = {
                            // Silently fail
                            Timber.d(it)
                        }
                    )
        }
    }

    private fun createBasicUser(offlineToken: NabuOfflineToken): Completable =
        nabuDataManager.createBasicUser(
            view.firstName,
            view.lastName,
            view.dateOfBirth?.toISO8601DateString()
                ?: throw IllegalStateException("DoB has not been set"),
            offlineToken
        ).subscribeOn(Schedulers.io())
            .doOnComplete {
                userService.markAsStale()
            }

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstNameSet && lastNameSet && dateSet)
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }

    private fun Date.toCalendar(): Calendar =
        Calendar.getInstance().apply { time = this@toCalendar }
}

private fun <T> Maybe<T>.toOptional(): Single<Optional<T>> =
    map { Optional.of(it) }
        .switchIfEmpty(Maybe.just(Optional.absent<T>()))
        .toSingle()
