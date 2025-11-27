package com.officesuite.app.utils

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Extension function to easily connect TabLayout with ViewPager2 as page indicator.
 */
fun TabLayout.setViewPager2(viewPager: ViewPager2) {
    TabLayoutMediator(this, viewPager) { _, _ ->
        // Empty - we just need the tabs as indicators
    }.attach()
}
