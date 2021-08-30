package com.blockchain.componentlib.demo.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.FragmentMenuBinding
import com.blockchain.componentlib.demo.DemoNavigation

class MenuFragment : Fragment(R.layout.fragment_menu) {
    private lateinit var binding: FragmentMenuBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMenuBinding.bind(view)

        setupUi()
    }

    private fun setupUi() {
        binding.mainMenuTypography.setOnClickListener {
            (activity as? DemoNavigation)?.goToTypography()
        }

        binding.mainMenuComponentsAll.setOnClickListener {
            (activity as? DemoNavigation)?.goToComponents()
        }
    }
}