package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.expandables.ExpandableItemView
import piuk.blockchain.blockchain_component_library_catalog.R

class ExpandablesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expandables)

        findViewById<ExpandableItemView>(R.id.expandable_item).apply {
            expandableText =
                "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been " +
                    "the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley."
            numOfVisibleLines = 2
            buttonTextToExpand = "Read More"
            buttonTextToCollapse = "Read Less"
        }
    }
}