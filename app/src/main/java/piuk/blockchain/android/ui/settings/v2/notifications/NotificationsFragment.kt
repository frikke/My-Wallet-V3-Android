package piuk.blockchain.android.ui.settings.v2.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentNotificationsBinding
import piuk.blockchain.android.ui.settings.v2.SettingsNavigator
import piuk.blockchain.android.ui.settings.v2.SettingsScreen

class NotificationsFragment :
    MviFragment<NotificationsModel, NotificationsIntent, NotificationsState, FragmentNotificationsBinding>(),
    SettingsScreen {

    override val model: NotificationsModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNotificationsBinding =
        FragmentNotificationsBinding.inflate(inflater, container, false)

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateToolbar(
            toolbarTitle = getString(R.string.notifications_toolbar),
            menuItems = emptyList()
        )

        with(binding) {
            emailNotifications.apply {
                primaryText = getString(R.string.email_notifications_title)
                onCheckedChange = {
                    model.process(NotificationsIntent.ToggleEmailNotifications)
                }
            }

            pushNotifications.apply {
                primaryText = getString(R.string.push_notifications_title)
                onCheckedChange = {
                    model.process(NotificationsIntent.TogglePushNotifications)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(NotificationsIntent.LoadNotificationInfo)
    }

    override fun render(newState: NotificationsState) {
        with(binding) {
            emailNotifications.isChecked = newState.emailNotificationsEnabled
            pushNotifications.isChecked = newState.pushNotificationsEnabled
        }

        if (newState.errorState != NotificationsError.NONE) {
            model.process(NotificationsIntent.ResetErrorState)
            when (newState.errorState) {
                NotificationsError.EMAIL_NOTIFICATION_UPDATE_FAIL ->
                    showSnackbar(SnackbarType.Error, R.string.notifications_email_update_error)
                NotificationsError.PUSH_NOTIFICATION_UPDATE_FAIL ->
                    showSnackbar(SnackbarType.Error, R.string.notifications_push_update_error)
                NotificationsError.NOTIFICATION_INFO_LOAD_FAIL -> {
                    showSnackbar(SnackbarType.Error, R.string.notifications_info_load_error)
                    with(binding) {
                        emailNotifications.toggleEnabled = false
                        pushNotifications.toggleEnabled = false
                    }
                }
                NotificationsError.EMAIL_NOT_VERIFIED ->
                    showSnackbar(SnackbarType.Info, R.string.notifications_email_unverified_error)
                NotificationsError.NONE -> {
                    // do nothing
                }
            }
        }
    }

    private fun showSnackbar(type: SnackbarType, @StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = type
        ).show()
    }

    companion object {
        fun newInstance() = NotificationsFragment()
    }
}
