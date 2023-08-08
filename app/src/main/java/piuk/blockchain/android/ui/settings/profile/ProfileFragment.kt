package piuk.blockchain.android.ui.settings.profile

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.base.updateTitleToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.capitalizeFirstChar
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentProfileBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.urllinks.PRIVATE_KEY_EXPLANATION
import piuk.blockchain.android.util.StringUtils

class ProfileFragment :
    MviFragment<ProfileModel, ProfileIntent, ProfileState, FragmentProfileBinding>(),
    ProfileNavigatorScreen {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun navigator(): ProfileNavigator =
        (activity as? ProfileNavigator) ?: throw IllegalStateException(
            "Parent must implement ProfileNavigator"
        )

    private val basicProfileInfo by lazy {
        arguments?.getSerializable(SettingsActivity.BASIC_INFO) as BasicProfileInfo
    }

    private val userTier by lazy {
        arguments?.getSerializable(SettingsActivity.USER_TIER) as KycTier
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTitleToolbar(getString(com.blockchain.stringResources.R.string.profile_toolbar))
        setupTierInfo(basicProfileInfo)
        setContactSupport()
    }

    override fun onResume() {
        super.onResume()
        model.process(ProfileIntent.LoadProfile)
    }

    override fun render(newState: ProfileState) {
        if (!newState.isLoading) {
            setProfileRowsInfo(basicProfileInfo, newState.userInfoSettings)
        }
    }

    private fun setupTierInfo(basicProfileInfo: BasicProfileInfo) {
        with(binding) {
            if (userTier == KycTier.BRONZE) {
                userInitials.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.bkgd_profile_circle_empty
                    )
                }
            } else {
                userInitials.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.bkgd_profile_circle
                    )
                    text = getString(
                        com.blockchain.stringResources.R.string.settings_initials,
                        basicProfileInfo.firstName.first().uppercase(),
                        basicProfileInfo.lastName.first().uppercase()
                    )
                }
            }
        }
    }

    private fun setProfileRowsInfo(
        basicProfileInfo: BasicProfileInfo?,
        userInfoSettings: WalletSettingsService.UserInfoSettings?
    ) {
        with(binding) {
            if (userTier != KycTier.BRONZE) {
                nameRow.apply {
                    visible()
                    primaryText = getString(com.blockchain.stringResources.R.string.profile_label_name)
                    secondaryText = getString(
                        com.blockchain.stringResources.R.string.common_spaced_strings,
                        basicProfileInfo?.firstName?.capitalizeFirstChar().orEmpty(),
                        basicProfileInfo?.lastName?.capitalizeFirstChar().orEmpty()
                    )
                    endImageResource = ImageResource.None
                }
            }

            div2.visibleIf { userTier != KycTier.BRONZE }
            contactSupport.visibleIf { userTier != KycTier.BRONZE }

            emailRow.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.profile_label_email)
                secondaryText = userInfoSettings?.email.orEmpty()
                onClick = {
                    navigator().goToUpdateEmailScreen(true)
                }
            }

            mobileRow.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.profile_label_mobile)
                secondaryText = if (userInfoSettings?.mobileWithPrefix.isNullOrEmpty()) {
                    getString(com.blockchain.stringResources.R.string.profile_mobile_empty)
                } else {
                    userInfoSettings?.mobileWithPrefix
                }
                onClick = { navigator().goToUpdatePhoneScreen(true) }
            }

            updateTags(
                emailVerified = userInfoSettings?.emailVerified ?: false,
                mobileVerified = userInfoSettings?.mobileVerified ?: false
            )
        }
    }

    private fun updateTags(emailVerified: Boolean, mobileVerified: Boolean) {
        val typeEmailTag = if (emailVerified) TagType.Success() else TagType.Warning()
        val textEmailTag =
            if (emailVerified) {
                getString(com.blockchain.stringResources.R.string.verified)
            } else {
                getString(
                    com.blockchain.stringResources.R.string.not_verified
                )
            }

        val typeMobileTag = if (mobileVerified) TagType.Success() else TagType.Warning()
        val textMobileTag =
            if (mobileVerified) {
                getString(com.blockchain.stringResources.R.string.verified)
            } else {
                getString(
                    com.blockchain.stringResources.R.string.not_verified
                )
            }

        with(binding) {
            emailRow.tags = listOf(
                TagViewState(
                    value = textEmailTag,
                    type = typeEmailTag
                )
            )
            mobileRow.tags = listOf(
                TagViewState(
                    value = textMobileTag,
                    type = typeMobileTag
                )
            )
        }
    }

    private fun setContactSupport() {
        val map = mapOf("contact_support" to Uri.parse(PRIVATE_KEY_EXPLANATION))
        val contactSupportText = StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            com.blockchain.stringResources.R.string.profile_label_support,
            map
        ) { onSupportClicked() }
        binding.contactSupport.apply {
            text = contactSupportText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun onSupportClicked() {
        analytics.logEvent(AnalyticsEvents.Support)
        startActivity(
            SupportCentreActivity.newIntent(
                context = requireContext(),
                subject = CHANGE_NAME_SUPPORT
            )
        )
    }

    companion object {
        private const val CHANGE_NAME_SUPPORT = "Update name and surname"

        fun newInstance(basicProfileInfo: BasicProfileInfo, tier: KycTier) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(SettingsActivity.BASIC_INFO, basicProfileInfo)
                    putSerializable(SettingsActivity.USER_TIER, tier)
                }
            }
    }
}
