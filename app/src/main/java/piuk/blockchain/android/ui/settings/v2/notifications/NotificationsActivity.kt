package piuk.blockchain.android.ui.settings.v2.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityNotificationsBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast

class NotificationsActivity :
    MviActivity<NotificationsModel, NotificationsIntent, NotificationsState, ActivityNotificationsBinding>() {

    override val model: NotificationsModel by scopedInject()

    override fun initBinding(): ActivityNotificationsBinding = ActivityNotificationsBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()

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
                    toast(getString(R.string.notifications_email_update_error), ToastCustom.TYPE_ERROR)
                NotificationsError.PUSH_NOTIFICATION_UPDATE_FAIL ->
                    toast(getString(R.string.notifications_push_update_error), ToastCustom.TYPE_ERROR)
                NotificationsError.NOTIFICATION_INFO_LOAD_FAIL -> {
                    toast(getString(R.string.notifications_info_load_error), ToastCustom.TYPE_ERROR)
                    with(binding) {
                        emailNotifications.toggleEnabled = false
                        pushNotifications.toggleEnabled = false
                    }
                }
                NotificationsError.EMAIL_NOT_VERIFIED ->
                    toast(getString(R.string.notifications_email_unverified_error), ToastCustom.TYPE_GENERAL)
                NotificationsError.NONE -> {
                    // do nothing
                }
            }
        }
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.notifications_toolbar),
            backAction = { onBackPressed() }
        )
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, NotificationsActivity::class.java)
    }
}
