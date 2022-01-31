package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sectionheader.LargeSectionHeaderType
import com.blockchain.componentlib.sectionheader.LargeSectionHeaderView
import com.blockchain.componentlib.sectionheader.BalanceSectionHeaderView
import com.blockchain.componentlib.sectionheader.SmallSectionHeaderView
import piuk.blockchain.blockchain_component_library_catalog.R

class SectionHeadersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_headers)

        findViewById<SmallSectionHeaderView>(R.id.small).apply {
            title = "Title"
        }

        findViewById<BalanceSectionHeaderView>(R.id.balance).apply {
            primaryText = "\$12,293.21"
            secondaryText = "0.1393819 BTC"
            onIconClick = {
                Toast.makeText(this@SectionHeadersActivity, "Clicked", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<LargeSectionHeaderView>(R.id.basic).apply {
            this.sectionHeader = LargeSectionHeaderType.Default("Title")
        }

        findViewById<LargeSectionHeaderView>(R.id.icon).apply {
            this.sectionHeader = LargeSectionHeaderType.Icon(
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

        findViewById<LargeSectionHeaderView>(R.id.filter).apply {
            this.sectionHeader = LargeSectionHeaderType.Filter(
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