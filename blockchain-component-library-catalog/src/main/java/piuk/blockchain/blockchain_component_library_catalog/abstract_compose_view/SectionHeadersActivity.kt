package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.sectionheader.ExchangeSectionHeaderType
import com.blockchain.componentlib.sectionheader.ExchangeSectionHeaderView
import com.blockchain.componentlib.sectionheader.WalletBalanceSectionHeaderView
import com.blockchain.componentlib.sectionheader.WalletSectionHeaderView
import piuk.blockchain.blockchain_component_library_catalog.R

class SectionHeadersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_headers)

        findViewById<WalletSectionHeaderView>(R.id.wallet).apply {
            title = "Title"
        }

        findViewById<WalletBalanceSectionHeaderView>(R.id.balance).apply {
            primaryText = "\$12,293.21"
            secondaryText = "0.1393819 BTC"
            buttonText = "Buy BTC"
            onButtonClick = {
                Toast.makeText(this@SectionHeadersActivity, "Clicked", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ExchangeSectionHeaderView>(R.id.basic).apply {
            this.sectionHeader = ExchangeSectionHeaderType.Default("Title")
        }

        findViewById<ExchangeSectionHeaderView>(R.id.icon).apply {
            this.sectionHeader = ExchangeSectionHeaderType.Icon(
                title = "Title",
                icon = ImageResource.Local(
                    id = R.drawable.ic_qr_code,
                    contentDescription = null
                ),
                onIconClicked = {
                    Toast.makeText(this@SectionHeadersActivity, "Icon clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<ExchangeSectionHeaderView>(R.id.filter).apply {
            this.sectionHeader = ExchangeSectionHeaderType.Filter(
                title = "Destination Address",
                options = listOf("USD", "GBP", "EUR"),
                onOptionSelected = {
                    Toast.makeText(this@SectionHeadersActivity, "option index $it", Toast.LENGTH_SHORT).show()
                },
                optionIndexSelected = 0
            )
        }
    }
}