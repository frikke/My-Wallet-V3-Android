package piuk.blockchain.android.ui.membership

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.membership.presentation.MembershipHubActivity
import com.blockchain.membership.presentation.MembershipHubActivity.Companion.START_ICON_CHANGE
import com.blockchain.membership.presentation.MembershipHubActivity.Companion.START_REFERRALS

class MembershipActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val membershipHubContract = registerForActivityResult(MembershipActivityResultContract()) {
        when (it) {
            MembershipActivityResult.StartIconUpdate -> appIconUpdateContract.launch(
                AppIconUpdateActivity.newIntent(this)
            )
            MembershipActivityResult.StartReferrals -> {
                //  TODO
            }
            null -> finish()
        }
    }

    private val appIconUpdateContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launchHub()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchHub()
    }

    private fun launchHub() {
        membershipHubContract.launch(MembershipActivityArgs())
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, MembershipActivity::class.java)
    }
}

class MembershipActivityArgs

sealed class MembershipActivityResult {
    object StartIconUpdate : MembershipActivityResult()
    object StartReferrals : MembershipActivityResult()
}

class MembershipActivityResultContract : ActivityResultContract<MembershipActivityArgs, MembershipActivityResult?>() {
    override fun createIntent(context: Context, input: MembershipActivityArgs): Intent =
        MembershipHubActivity.newIntent(context)

    override fun parseResult(resultCode: Int, intent: Intent?): MembershipActivityResult? {
        val startIconUpdate = intent?.getBooleanExtra(START_ICON_CHANGE, false) ?: false
        val startReferrals = intent?.getBooleanExtra(START_REFERRALS, false) ?: false

        return when {
            resultCode != Activity.RESULT_OK -> null
            startIconUpdate -> MembershipActivityResult.StartIconUpdate
            startReferrals -> MembershipActivityResult.StartReferrals
            else -> null
        }
    }
}
