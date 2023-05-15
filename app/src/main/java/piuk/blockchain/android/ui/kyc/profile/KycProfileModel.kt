package piuk.blockchain.android.ui.kyc.profile

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.util.toISO8601DateString
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.store.firstOutcome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel

sealed class Navigation : NavigationEvent {
    data class AddressVerification(val profileModel: ProfileModel) : Navigation()
}

@Parcelize
data class Args(
    val countryIso: CountryIso,
    val stateIso: StateIso?,
    val isCowboysUser: Boolean
) : ModelConfigArgs.ParcelableArgs

class KycProfileModel(
    private val analytics: Analytics,
    private val nabuDataManager: NabuDataManager,
    private val userService: UserService,
    private val getUserStore: GetUserStore
) : MviViewModel<
    KycProfileIntent,
    KycProfileViewState,
    KycProfileModelState,
    Navigation,
    Args
    >(KycProfileModelState()) {

    private lateinit var countryIso: CountryIso
    private var stateIso: StateIso? = null
    private var isCowboysUser: Boolean = false

    override fun viewCreated(args: Args) {
        countryIso = args.countryIso
        stateIso = args.stateIso
        isCowboysUser = args.isCowboysUser

        viewModelScope.launch {
            userService.getUserFlow(refreshStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
                // TODO(aromano): remove mapping once UserService returns DataResources
                .map { DataResource.Data(it) as DataResource<NabuUser> }
                .catch { emit(DataResource.Error(it as Exception)) }
                .firstOutcome()
                .doOnSuccess { user ->
                    updateState {
                        // Don't restore data if data already present, as it'll overwrite what the user
                        // may have edited themselves
                        if (
                            it.firstNameInput.isNotEmpty() ||
                            it.lastNameInput.isNotEmpty() ||
                            it.dateOfBirthInput != null
                        ) {
                            it
                        } else {
                            val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val dateOfBirth = try {
                                val date = backendFormat.parse(user.dob!!)!!
                                Calendar.getInstance().apply {
                                    time = date
                                }
                            } catch (ex: Exception) {
                                null
                            }

                            it.copy(
                                firstNameInput = user.firstName.orEmpty(),
                                lastNameInput = user.lastName.orEmpty(),
                                dateOfBirthInput = dateOfBirth
                            )
                        }
                    }
                }
        }
    }

    override fun reduce(state: KycProfileModelState): KycProfileViewState = KycProfileViewState(
        firstNameInput = state.firstNameInput,
        lastNameInput = state.lastNameInput,
        isNameInputErrorShowing = state.isNameInputErrorShowing,
        dateOfBirthInput = state.dateOfBirthInput,
        continueButtonState = when {
            state.isSavingProfileLoading -> ButtonState.Loading
            state.firstNameInput.isBlank() -> ButtonState.Disabled
            state.lastNameInput.isBlank() -> ButtonState.Disabled
            state.dateOfBirthInput == null -> ButtonState.Disabled
            state.isNameInputErrorShowing -> ButtonState.Disabled
            else -> ButtonState.Enabled
        },
        error = state.error
    )

    override suspend fun handleIntent(modelState: KycProfileModelState, intent: KycProfileIntent) {
        when (intent) {
            is KycProfileIntent.FirstNameInputChanged -> updateState {
                it.copy(firstNameInput = intent.value, isNameInputErrorShowing = false)
            }
            is KycProfileIntent.LastNameInputChanged -> updateState {
                it.copy(lastNameInput = intent.value, isNameInputErrorShowing = false)
            }
            is KycProfileIntent.DateOfBirthInputChanged -> {
                updateState { it.copy(dateOfBirthInput = intent.value) }
                if (modelState.firstNameInput.isNotBlank() && modelState.lastNameInput.isNotBlank()) {
                    nabuDataManager.isProfileNameValid(modelState.firstNameInput, modelState.lastNameInput)
                        .doOnSuccess { isValid ->
                            updateState { it.copy(isNameInputErrorShowing = !isValid) }
                        }
                }
            }
            KycProfileIntent.ContinueClicked -> {
                updateState { it.copy(isSavingProfileLoading = true) }

                val dobDisplayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                val dob = modelState.dateOfBirthInput?.let { dobDisplayFormat.format(it.time) }.orEmpty()
                analytics.logEvent(
                    KYCAnalyticsEvents.PersonalDetailsSet(
                        "${modelState.firstNameInput}," +
                            "${modelState.lastNameInput}," +
                            dob
                    )
                )
                if (isCowboysUser) analytics.logEvent(CowboysAnalytics.KycPersonalInfoConfirmed)

                nabuDataManager.createBasicUser(
                    modelState.firstNameInput,
                    modelState.lastNameInput,
                    modelState.dateOfBirthInput!!.toISO8601DateString()
                ).doOnSuccess {
                    getUserStore.markAsStale()
                    val profile = ProfileModel(
                        firstName = modelState.firstNameInput,
                        lastName = modelState.lastNameInput,
                        countryCode = countryIso,
                        stateCode = stateIso
                    )
                    navigate(Navigation.AddressVerification(profile))
                }.doOnFailure {
                    val apiException = it as? NabuApiException
                    val error = when {
                        apiException?.getErrorStatusCode() == NabuErrorStatusCodes.Conflict ->
                            KycProfileError.UserConflict
                        apiException?.getErrorCode() == NabuErrorCodes.InvalidName ->
                            KycProfileError.InvalidName
                        else -> KycProfileError.Generic(it.message)
                    }

                    updateState {
                        it.copy(
                            error = error,
                            isNameInputErrorShowing = error == KycProfileError.InvalidName
                        )
                    }
                }
                updateState { it.copy(isSavingProfileLoading = false) }
            }
            KycProfileIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }
    }
}
