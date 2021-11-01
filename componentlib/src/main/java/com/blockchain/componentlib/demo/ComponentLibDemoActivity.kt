package com.blockchain.componentlib.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.databinding.ViewDemoActivityBinding
import com.blockchain.componentlib.demo.fragments.ComponentFragment
import com.blockchain.componentlib.demo.fragments.MenuFragment
import com.blockchain.componentlib.demo.fragments.TypographyFragment

class ComponentLibDemoActivity : AppCompatActivity(), DemoNavigation {
    private lateinit var binding: ViewDemoActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ViewDemoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        goToMenu()
    }

    override fun goToComponents() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, ComponentFragment())
            .addToBackStack(ComponentFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun goToMenu() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, MenuFragment())
            .addToBackStack(MenuFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun goToTypography() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, TypographyFragment())
            .addToBackStack(TypographyFragment::class.simpleName)
            .commitAllowingStateLoss()
    }
}