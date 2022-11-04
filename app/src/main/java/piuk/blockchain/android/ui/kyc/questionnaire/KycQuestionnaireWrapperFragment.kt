package piuk.blockchain.android.ui.kyc.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.domain.dataremediation.model.Questionnaire
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewFragmentContainerBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate

/**
 * This wrapper is used to facilitate Questionnaire usage in Kyc due to using jetpack Navigation and needing to pass countryCode along to Veriff
 */
class KycQuestionnaireWrapperFragment : Fragment(), QuestionnaireSheet.Host {

    private val fraudService: FraudService by inject()

    private val questionnaire: Questionnaire by lazy {
        KycQuestionnaireWrapperFragmentArgs.fromBundle(arguments ?: Bundle()).questionnaire
    }

    private val countryCode: String by lazy {
        KycQuestionnaireWrapperFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
    }

    private lateinit var binding: ViewFragmentContainerBinding

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ViewFragmentContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fraudService.trackFlow(FraudFlow.KYC)

        val hostNavBarButtons = if (!questionnaire.isMandatory) {
            listOf(
                NavigationBarButton.Text(
                    text = getString(R.string.common_skip),
                    color = Blue600,
                    onTextClick = ::questionnaireSkipped
                )
            )
        } else {
            emptyList()
        }
        progressListener.setupHostToolbar(R.string.kyc_additional_info_toolbar, hostNavBarButtons)
        if (childFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            childFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container,
                    QuestionnaireSheet.newInstance(questionnaire)
                ).commitAllowingStateLoss()
        }
    }

    override fun questionnaireSubmittedSuccessfully() {
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun questionnaireSkipped() {
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun onSheetClosed() {
    }
}
