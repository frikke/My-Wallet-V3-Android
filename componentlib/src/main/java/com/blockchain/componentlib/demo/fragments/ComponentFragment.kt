package com.blockchain.componentlib.demo.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.blockchain.componentlib.R
import com.blockchain.componentlib.carousel.CarouselViewType
import com.blockchain.componentlib.databinding.FragmentComponentsBinding

class ComponentFragment : Fragment(R.layout.fragment_components) {
    private lateinit var binding: FragmentComponentsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentComponentsBinding.bind(view)

        setupUi()
    }

    private fun setupUi() {
        with(binding) {
            carousel.submitList(
                listOf(
                    CarouselViewType.ValueProp(R.drawable.carousel_placeholder_1, "Carousel 1"),
                    CarouselViewType.ValueProp(R.drawable.carousel_placeholder_2, "Carousel 2"),
                    CarouselViewType.ValueProp(R.drawable.carousel_placeholder_3, "Carousel 3")
                )
            )
            carousel.setCarouselIndicator(carouselIndicator)
        }
    }
}
