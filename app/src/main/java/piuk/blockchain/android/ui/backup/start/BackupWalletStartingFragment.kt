package piuk.blockchain.android.ui.backup.start

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.presentation.BackupPhraseActivity
import com.blockchain.ui.password.SecondPasswordHandler
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBackupStartBinding
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.util.scopedInjectActivity

class BackupWalletStartingFragment :
    MviFragment<BackupWalletStartingModel,
        BackupWalletStartingIntents,
        BackupWalletStartingState,
        FragmentBackupStartBinding>() {

    private val backupFeatureFlag: FeatureFlag by inject(backupPhraseFeatureFlag)

    private val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()

    override val model: BackupWalletStartingModel by scopedInject()

    private var latestStatus: BackupWalletStartingStatus? = null

    private val onChangePinResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            pinCodeValidatedForChange()
        } else {
            model.process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.INIT))
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBackupStartBinding =
        FragmentBackupStartBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            model.process(
                BackupWalletStartingIntents.UpdateStatus(
                    status = BackupWalletStartingStatus.REQUEST_PIN
                )
            )
        }
    }

    override fun render(newState: BackupWalletStartingState) {
        if (latestStatus != newState.status) {
            when (newState.status) {
                BackupWalletStartingStatus.REQUEST_PIN -> showPinForVerification()
                else -> {}
            }
            latestStatus = newState.status
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PinActivity.REQUEST_CODE_VALIDATE_PIN -> {
                if (resultCode == RESULT_OK) {
                    pinCodeValidatedForChange()
                } else {
                    model.process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.INIT))
                }
            }
        }
    }

    private fun pinCodeValidatedForChange() {
        model.process(BackupWalletStartingIntents.TriggerEmailAlert)
        secondPasswordHandler.validate(
            requireContext(),
            object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    loadFragmentWordListFragment()
                }

                override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                    loadFragmentWordListFragment(validatedSecondPassword)
                }
            }
        )
    }

    private fun loadFragmentWordListFragment(secondPassword: String? = null) {
        backupFeatureFlag.enabled.subscribe { enabled ->
            if (enabled) {
                BackupPhraseActivity.newIntent(activity, secondPassword)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .also { intent -> startActivity(intent) }
            } else {
                val fragment = BackupWalletWordListFragment().apply {
                    secondPassword?.let {
                        arguments = Bundle().apply {
                            putString(
                                BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD,
                                it
                            )
                        }
                    }
                }
                activity.run {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    private fun showPinForVerification() {
        onChangePinResult.launch(
            PinActivity.newIntent(
                context = requireContext(),
                startForResult = true,
                originScreen = PinActivity.Companion.OriginScreenToPin.BACKUP_PHRASE,
                addFlagsToClear = false
            )
        )
    }

    companion object {
        const val TAG = "BackupWalletStartingFragment"
    }
}
