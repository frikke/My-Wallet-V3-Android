package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.navigation.BottomNavigationBarView
import com.blockchain.componentlib.navigation.BottomNavigationState
import com.google.android.material.button.MaterialButton
import piuk.blockchain.blockchain_component_library_catalog.R

class BottomNavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)


        val bottomNavigationBar = findViewById<BottomNavigationBarView>(R.id.default_bottom_navigation).apply {
            onNavigationItemClick = {
                selectedNavigationItem = it
            }
            onMiddleButtonClick = {
                bottomNavigationState = when (bottomNavigationState) {
                    BottomNavigationState.Add -> BottomNavigationState.Cancel
                    BottomNavigationState.Cancel -> BottomNavigationState.Add
                }
            }
        }

        findViewById<MaterialButton>(R.id.bottom_nav_set_add).apply {
            setOnClickListener {
                bottomNavigationBar.bottomNavigationState = BottomNavigationState.Add
            }
        }

        findViewById<MaterialButton>(R.id.bottom_nav_set_cancel).apply {
            setOnClickListener {
                bottomNavigationBar.bottomNavigationState = BottomNavigationState.Cancel
            }
        }

        findViewById<MaterialButton>(R.id.bottom_nav_enable_pulse).apply {
            setOnClickListener {
                bottomNavigationBar.isPulseAnimationEnabled = !bottomNavigationBar.isPulseAnimationEnabled
            }
        }


    }
}